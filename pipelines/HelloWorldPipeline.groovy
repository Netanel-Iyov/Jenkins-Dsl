pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
    }

    stages {
        stage('Clean workspace') {
            steps {
                container('jnlp') {
                    cleanWs()
                }
            }
        }
    }
}