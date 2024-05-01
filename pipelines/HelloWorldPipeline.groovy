pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp'
        }
    }

    stages {
        stage('Build') {
            steps {
                container('jnlp') {
                    sh 'echo "Hello World"'
                }
            }
        }
    }
}