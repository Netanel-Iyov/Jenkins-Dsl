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
        stage('Main') {
            steps {
                sh 'echo "Hello World from Jenkinsfile"'
            }
        }
    }
}

pipelineJob('my-pipeline') {
    // definition {
    //     cpsScm {
    //         scm {
    //             git {
    //                 remote {
    //                     url('https://github.com/your-username/your-repo.git')
    //                 }
    //                 branch('*/main')
    //             }
    //         }
    //     }
    // }

    // triggers {
    //     scm('H/5 * * * *') // Poll every 5 minutes
    // }

    // parameters {
    //     stringParam('DEPLOYMENT_ENV', 'staging', 'Environment to deploy to')
    // }

    pipeline {
        agent any

        stages {
            stage('Build') {
                steps {
                    sh 'echo "Hello World"'
                }
            }
        }
    }
}