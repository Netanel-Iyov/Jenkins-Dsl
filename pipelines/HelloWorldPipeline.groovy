pipeline {
    agent {
        kubernetes {
            // TODO: remove from here and extract to a different file
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
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
        }
    }


    stages {
        stage('Checkout') {
            steps {
                container('jnlp') {
                    cleanWs()
                    git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-list'
                    script {
                        sh 'ls -la'
                    }
                }
            }
        }

        // stage('Test') {

        // }

        stage('Build') {
            steps {
                container('docker') {
                    dir('client') {
                        script {
                            sh 'ls -la'
                            sh 'docker build . -f Dockerfile.prod'
                        }
                    }
                }
            }
        }

        // stage('Push To Registry') {

        // }
    }
}