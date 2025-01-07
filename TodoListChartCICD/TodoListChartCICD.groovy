pipeline {
    agent {
        kubernetes {
            yaml readFile('TodoListChartCiCD/K8SPod.yaml')
            defaultContainer 'shell'
        }
    }

    stages {
        stage('Checkout') {
            steps {
                dir('Todo-list') {
                    cleanWs()
                    checkout scmGit(branches: [[name: "refs/tags/${RELEASE_TAG}"]], userRemoteConfigs: [[credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-list']])
                    script {
                        sh 'echo "Display Checkout content" && ls -la'
                    }
                }
            }
        }
    }
}