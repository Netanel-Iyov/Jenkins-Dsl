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
                }
            }
        }

        stage ('Debug env var settings') {
            steps {
                script {
                    sh 'env'
                }
            }
        }

        // stage('Checkout') {
        //     steps {
        //         dir('Todo-list') {
        //             cleanWs()
        //             checkout scmGit(branches: [[name: "refs/tags/${RELEASE_TAG}"]], userRemoteConfigs: [[credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-list']])
        //             script {
        //                 sh 'echo "Display Checkout content" && ls -la'
        //             }
        //         }
        //     }
        // }

        // stage("Build API & Push To Registry") {
        //     steps {
        //         dir("Todo-list/api") {
        //             script {
        //                 container('docker') {
        //                     docker.withRegistry(DOCKER_REGISTRY, DOCKER_REGISTRY_CREDENTIALS) {
        //                         def tagPrefix = [
        //                             'released': '',
        //                             'prereleased': '-staging'
        //                         ]
        //                         def imageTag = "${DOCKER_REPOSITORY}/todo-list-client${tagPrefix[env.ACTION]}:${RELEASE_TAG}"
        //                         def dockerImage = docker.build(imageTag, "-f Dockerfile.prod .")
        //                         dockerImage.push()
        //                     }
        //                 }
        //             }
        //         }
        //     }
        // }

        // stage("Build Client & Push To Registry") {
        //     steps {
        //         dir("Todo-list/client") {
        //             script {
        //                 if (applicationValues.client.toDeploy) {
        //                     container('docker') {
        //                         // Use withCredentials to retrieve the secret file
        //                         withCredentials([file(credentialsId: 'Todo-List-React-.env-File', variable: 'ENV_FILE')]) {
        //                             // Copy the secret file to the desired location in the workspace
        //                             sh "cp \$ENV_FILE ./.env.prod"
        //                         }
                                
        //                         docker.withRegistry('https://registry.hub.docker.com', 'DockerHub-Credentials') {
        //                             def dockerImage = docker.build(applicationValues.client.tag, "-f Dockerfile.prod .")
        //                             dockerImage.push()
        //                         }
        //                     }
        //                 } else {
        //                     Utils.markStageSkippedForConditional(STAGE_NAME)
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