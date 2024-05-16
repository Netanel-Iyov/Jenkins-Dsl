pipeline {
   agent {
        kubernetes {
            // TODO: remove from here and extract to a different file
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: python
    image: python:3.9
    command:
    - sleep
    args:
    - infinity
'''
            defaultContainer 'python'
        }
    } 

    parameters {
        string(name: 'BRANCH', description: 'Git branch to checkout on', defaultValue: 'main')
        string(name: 'RECORD_NAMES', description: 'Select Record Names (seperated by space)', defaultValue: '@ jenkins argocd')
    }
    
    triggers {
        cron('*/30 * * * *')  // Run the build every half an hour
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                echo "branch is ${BRANCH}"
                git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Home-Server.git', branch: 'dns-records-go-daddy-creds-as-arguments' // "${BRANCH}"
                script {
                    sh 'ls -la'
                }
            }
        }

        stage('Update DNS') {
            steps {
                withCredentials([string(credentialsId: 'go-daddy-api-key', variable: 'api-key'), string(credentialsId: 'go-daddy-api-secret', variable: 'api-secret') ]) 
                {
                    sh "pip install requests && python3 ./misc/update_DNS_record.py --domain niyov.com --record-names ${RECORD_NAMES} --api-key ${api-key} --api-secret ${api-secret}"
                }
            }
        }
        
    }
}
