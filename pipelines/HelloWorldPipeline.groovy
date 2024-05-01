pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp-pod-agent'
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