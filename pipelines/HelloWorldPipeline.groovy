pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
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

        stage('Push To Registry') {

        }
    }
}