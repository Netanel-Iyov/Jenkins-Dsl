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
        string(name: 'RECORD_NAMES', description: 'Select Record Names (seperated by space)', defaultValue: 'jenkins.niyov.com argocd.niyov.com todo-list.niyov.com api-todo-list.niyov.com')
    }
    
    triggers {
        cron('*/30 * * * *')  // Run the build every half an hour
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                echo "branch is ${BRANCH}"
                git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Home-Server.git', branch: "${BRANCH}"
            }
        }

        stage('Update DNS') {
            steps {
                dir('Home-Server/update_DNS_records') {
                    withCredentials([string(credentialsId: 'Cloudflare-Global-API-Key', variable: 'API_KEY')]) {
                    sh """
                        pip install -r requirements.txt
                        python ./update_DNS_record.py --api-key ${API_KEY} --email nati16368447@gmail.com --hostnames ${}
                    """
                    }
                }
            }
        }
        
    }
}
