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
    }
}