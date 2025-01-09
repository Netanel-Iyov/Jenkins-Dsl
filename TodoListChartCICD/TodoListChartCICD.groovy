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
                        case 'released':
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
                            error 'Not a Valid Version! Please fix the release/prerelease in Github.'
                    }

                    env.IMAGE_TAG = env.RELEASE_TAG.substring(1)
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
                        sh 'echo "Display Checkout content" && ls -la'
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
                                        def dockerImage = docker.build(imageTag, "-f Dockerfile.prod .")
                                        dockerImage.push()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        // stage("Build Client & Push To Registry") {
        //     steps {
        //         dir("Todo-list/client") {
        //             script {
        //                 container('docker') {
        //                     // Use withCredentials to retrieve the secret file
        //                     withCredentials([file(credentialsId: 'Todo-List-React-.env-File', variable: 'ENV_FILE')]) {
        //                         // Copy the secret file to the desired location in the workspace
        //                         sh "cp \$ENV_FILE ./.env.prod"
        //                     }
        //                     def tagPrefix = [
        //                         'production': '',
        //                         'staging': '-staging',
        //                         'testing': '-testing'
        //                     ]
        //                     def imageTag = "${DOCKER_REPOSITORY}/todo-list-client${tagPrefix[env.RELEASE_ENVIRONMENT]}:${IMAGE_TAG}"
        //                     docker.withRegistry(DOCKER_REGISTRY, DOCKER_REGISTRY_CREDENTIALS) {
        //                         def dockerImage = docker.build(imageTag, "-f Dockerfile.prod .")
        //                         dockerImage.push()
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // } 

        // stage('Checkout ArgoCD Repository') {
        //     steps {
        //         dir('argocd-todolist') {
        //             git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/todolist-argocd', branch: 'main'
        //         }
        //     }
        // }

    }
}