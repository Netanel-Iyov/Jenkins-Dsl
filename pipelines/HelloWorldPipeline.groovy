pipeline {
    agent {
        kubernetes {
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
      type: Directory
'''
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
                    dir('api') {
                        script {
                            sh 'ls -la'
                            sh 'docker build .'
                        }
                    }
                }
            }
        }

        // stage('Push To Registry') {

        // }
    }
}