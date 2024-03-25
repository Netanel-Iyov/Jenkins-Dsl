pipeline {
    agent {
        kubernetes {
            inheritFrom 'jnlp-pod-agent'
        }
    }

    stages {
        stage('Build') {
            container('jnlp') {
                steps {
                    sh 'echo "Hello World"'
                }
            }
        }
    }
}