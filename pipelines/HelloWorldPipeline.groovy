pipeline {
    agent {
        kubernetes {
            // inheritFrom 'jnlp-pod-agent'
            containerTemplate {
                name 'jnlp'
                image 'jenkins/inbound-agent:4.13.3-1'
            }
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