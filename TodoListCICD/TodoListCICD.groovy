pipeline {
    agent {
        kubernetes {
            // TODO: remove from here and extract to a different file
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
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

    environment {
        DOCKER_IMAGE = "todolist-cicd-test:${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-list'
                script {
                    sh 'ls -la'
                }
            }
        }

        // stage('Test') {

        // }

        stage('Build & Push To Registry') {
            steps {
                container('docker') {
                    dir('api') {
                        script {
                            sh 'ls -la'
                            sh 'docker build -f Dockerfile.prod -t ${DOCKER_IMAGE} .'
                            def dockerImage = docker.image("${DOCKER_IMAGE}")
                            // docker.withRegistry('https://harbor.niyov.com', "harbor.niyov-credentials") {
                                // dockerImage.push()
                        }
                    }
                }
            }
        }

        // stage('Update Deployment File') {
        //     steps {
        //         script {
                    
        //         }
        //     }
        // }
        
    }
}