// Uses Declarative syntax to run commands inside a container.
pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: shell
    image: ubuntu
    command:
    - sleep
    args:
    - infinity
'''
            defaultContainer 'shell'
        }
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                withCredentials([string(credentialsId: 'Personal-Github-Token', variable: 'GITHUB_TOKEN')]) { 
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            url: "https://${GITHUB_TOKEN}@github.com/Netanel-Iyov/Jenkins-Dsl.git"
                        ]]
                    ])
                }
            }
        }

        stage('Seed All') {
            steps {
                jobDsl removedConfigFilesAction: 'DELETE', removedJobAction: 'DELETE', removedViewAction: 'DELETE', targets: 'pipelines/DSL**.groovy'
            }
        }
    }
}
