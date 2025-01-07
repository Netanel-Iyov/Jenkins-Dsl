pipeline {
    agent {
        kubernetes {
            yaml '''
apiVersion: v1
kind: Pod
spec:
  containers:
    - name: git
      image: alpine/git:2.45.2
      command:
        - sleep
      args:
        - infinity
    - name: shell
      image: ubuntu
      command:
        - sleep
      args:
        - infinity
    - name: docker
      image: docker:latest
      command:
        - "sleep"
      args:
        - "99d"
      volumeMounts:
        - name: docker-daemon
          mountPath: /var/run/docker.sock
      tty: true
  volumes:
    - name: docker-daemon
      hostPath:
        path: /var/run/docker.sock
        type: Socket
            '''
            defaultContainer 'shell'
        }
    }

    stages {
        stage('Checkout') {
            steps {
                dir('Todo-list') {
                    cleanWs()
                    checkout scmGit(branches: [[name: 'refs/tags/${tag}']], userRemoteConfigs: [[credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-list']])
                    script {
                        sh 'echo "Display Checkout content" && ls -la'
                    }
                }
            }
        }
    }
}