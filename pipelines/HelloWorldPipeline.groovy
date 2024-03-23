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
        stage('Build') {
            steps {
                sh 'echo "Hello World"'
            }
        }
    }
}