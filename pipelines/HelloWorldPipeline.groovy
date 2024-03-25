pipeline {
    agent {
        kubernetes {
            label 'jnlp-pod-agent'
        }
    }

    stages {
        stage('Build') {
            steps {
                sh 'echo "Hello World"'
            }
        }
    }
}