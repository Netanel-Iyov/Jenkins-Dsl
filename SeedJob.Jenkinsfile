// Uses Declarative syntax to run commands inside a container.
pipeline {
    parameters {
        string(name: 'BRANCH', description: 'DSL Branch To Checkout On')
    }

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

                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "*/${params.BRANCH}"]],
                    userRemoteConfigs: [
                        [ 
                            url: "https://github.com/Netanel-Iyov/Jenkins-Dsl.git",
                            credentialsId: 'Github-Credentials'
                        ]
                    ]
                ])
            }
        }

        stage('Seed All') {
            steps {
                jobDsl removedConfigFilesAction: 'DELETE', removedJobAction: 'DELETE', removedViewAction: 'DELETE', targets: '*/DSL**.groovy'
            }
        }
    }
}
