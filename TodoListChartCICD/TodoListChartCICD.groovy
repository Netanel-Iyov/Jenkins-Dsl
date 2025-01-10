pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: git
      image: alpine/git:2.45.2
      command:
        - sleep
      args:
        - infinity
    - name: shell
      image: ubuntu
      command:
        - sleep
      args:
        - infinity
    - name: docker
      image: docker:latest
      command:
        - "sleep"
      args:
        - "99d"
      volumeMounts:
        - name: docker-daemon
          mountPath: /var/run/docker.sock
      tty: true
  volumes:
    - name: docker-daemon
      hostPath:
        path: /var/run/docker.sock
        type: Socket
            '''
            defaultContainer 'shell'
        }
    }

    stages {
        stage('Setup') {
            steps {
                script {
                    env.DOCKER_REGISTRY = 'https://harbor.niyov.com'
                    env.DOCKER_REGISTRY_CREDENTIALS = 'Private-Harbor-Credentials'
                    env.DOCKER_REPOSITORY = 'harbor.niyov.com/applications'

                    switch(env.ACTION) {
                        case 'released':
                            env.RELEASE_ENVIRONMENT = 'production'
                            break
                        case 'prereleased':
                            env.RELEASE_ENVIRONMENT = 'staging' 
                            break
                        case null:
                            env.RELEASE_ENVIRONMENT = 'testing'
                            break
                    }

                    // Verify tag version
                    if (env.RELEASE_ENVIRONMENT == 'staging' || env.RELEASE_ENVIRONMENT == 'production') {
                        def pattern = ~/^v\d+\.\d+\.\d+$/
                        def validVersion = env.RELEASE_TAG ==~ pattern
                        if (!validVersion)
                            error "RELEASE_TAG: ${RELEASE_TAG} is Not a Valid Version! Please fix the release/prerelease in Github."
                        env.IMAGE_TAG = env.RELEASE_TAG.substring(1)
                    } 
                }
            }
        }

        stage('Checkout') {
            steps {
                dir('Todo-list') {
                    script {
                        cleanWs()
                        def ref = RELEASE_ENVIRONMENT == 'testing' ? "refs/heads/${params.BRANCH}" : "refs/tags/${RELEASE_TAG}"
                        checkout scmGit(branches: [[name: ref]], userRemoteConfigs: [[credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-list']])
                        
                        // set the image tag to be the commit hash in case of testing env deployment
                        if (RELEASE_ENVIRONMENT == 'testing') {
                            env.IMAGE_TAG = sh('git rev-parse HEAD', returnStdout: true).trim() 
                        }
                    }
                }
            }
        }

        stage('Build') {
            parallel {
                stage("Build API & Push To Registry") {
                    steps {
                        dir("Todo-list/api") {
                            script {
                                container('docker') {
                                    docker.withRegistry(DOCKER_REGISTRY, DOCKER_REGISTRY_CREDENTIALS) {
                                        def tagPrefix = [
                                            'production': '',
                                            'staging': '-staging',
                                            'testing': '-testing'
                                        ]
                                        def imageTag = "${DOCKER_REPOSITORY}/todo-list-api${tagPrefix[env.RELEASE_ENVIRONMENT]}:${IMAGE_TAG}"
                                        def dockerImage = docker.build(imageTag, "--no-cache -f Dockerfile.prod .")
                                        // dockerImage.push()

                                        env.API_TAG = imageTag
                                    }
                                }
                            }
                        }
                    }
                }
            
                stage("Build Client & Push To Registry") {
                    steps {
                        dir("Todo-list/client") {
                            script {
                                container('docker') {
                                    // Use withCredentials to retrieve the secret file
                                    def envFile = [
                                        'production': 'Production-Todo-List-React-.env-File',
                                        'staging': 'Staging-Todo-List-React-.env-File',
                                        'testing': 'Testing-Todo-List-React-.env-File'
                                    ]
                                    def tagPrefix = [
                                        'production': '',
                                        'staging': '-staging',
                                        'testing': '-testing'
                                    ]

                                    withCredentials([file(credentialsId: envFile[env.RELEASE_ENVIRONMENT], variable: 'ENV_FILE')]) {
                                        // Copy the secret file to the desired location in the workspace
                                        sh "cp \$ENV_FILE ./.env.prod"
                                    }

                                    def imageTag = "${DOCKER_REPOSITORY}/todo-list-client${tagPrefix[env.RELEASE_ENVIRONMENT]}:${IMAGE_TAG}"
                                    docker.withRegistry(DOCKER_REGISTRY, DOCKER_REGISTRY_CREDENTIALS) {
                                        def dockerImage = docker.build(imageTag, "--no-cache -f Dockerfile.prod .")
                                        // dockerImage.push()

                                        env.CLIENT_TAG = imageTag
                                    }
                                }
                            }
                        }
                    }
                } 
            }
        }

        stage('Checkout Todo-List Chart Repository') {
            steps {
                container('git') {
                    dir('Todo-List-Chart') {
                        git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-List-Chart.git', branch: 'main'
                    }
                }
            }
        }

        stage('Update ArgoCD Files') {
            steps {
                dir('Todo-List-Chart') {
                    script {
                        def fileToUpdateMap = [
                            'production': 'values-prod.yaml',
                            'staging': 'values-staging.yaml',
                            'testing': 'values-testing.yaml'
                        ]

                        // Read the YAML file into a string
                        def fileToUpdate = fileToUpdateMap[env.RELEASE_ENVIRONMENT]
                        def yamlContent = readFile(fileToUpdate)
                        
                        // Replace the image tags using the environment variables
                        yamlContent = yamlContent.replaceAll(/${DOCKER_REPOSITORY}\/todo-list-client.*:\d+\.\d+\.\d+/, env.CLIENT_TAG)
                        yamlContent = yamlContent.replaceAll(/${DOCKER_REPOSITORY}\/todo-list-api.*:\d+\.\d+\.\d+/, env.API_TAG)
                        
                        // Write the modified YAML content back to the file
                        writeFile file: fileToUpdate, text: yamlContent, overwrite: true
                        
                        echo "${fileToUpdate} file has been updated with new versions..."
                        sh "cat ${fileToUpdate}"
                    }
                }
            }
        }
    }
}