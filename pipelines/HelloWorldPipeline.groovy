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
                }
            }
        }
    }
}