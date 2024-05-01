pipeline {
    agent {
        kubernetes {
            inheritFrom 'todo-list-CICD'
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