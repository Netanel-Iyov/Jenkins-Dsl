// Uses Declarative syntax to run commands inside a container.
pipeline {
    parameters {
        string(name: 'REF', description: 'Push Event Branch Ref')
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
                    branches: [[name: "*/${BRANCH}"]],
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
