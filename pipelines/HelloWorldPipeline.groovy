pipeline {
    agent {
        kubernetes {
            label 'jnlp-pod-agent'
            defaultContainer 'jnlp'
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