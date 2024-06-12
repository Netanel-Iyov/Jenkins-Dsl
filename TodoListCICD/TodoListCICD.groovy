pipeline {
    agent {
        kubernetes {
            yamlFile 'KubernetesPod.yaml'
            defaultContainer 'shell'
        }
    }

    environment {
        CLIENT_IMAGE_TAG=""
        API_IMAGE_TAG=""
    }

    stages {
        stage('Checkout') {
            steps {
                cleanWs()
                git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-list'
                script {
                    sh 'echo "Display Checkout content" && ls -la'
                }
            }
        }

        stage ('Verify Versions') {
            // Check github triggers and regexes
            // 1. define the api and client tags, for example: 'natiiyov/todo-list-client:1.0.0'
            // 2. get changed files and sort by /api, /client
            // 3. extract versions from the versions.json file in the repository
            // 4. for each tag see if changed files in the relevant directory, if changed check version
            // 5. if both tags are valid then continue the pipeline.
            // 6. if not valid, then notify somehow and exit the pipeline with Failed status code
        }

        // stage('Test') {

        // }

        stage('Build API & Push To Registry') {
            steps {
                container('docker') {
                    dir('api') {
                        script {
                            sh 'ls -la'
                            sh 'docker build -f Dockerfile.prod -t ${API_IMAGE_TAG} .'
                            
                            def dockerImage = docker.image("${DOCKER_IMAGE}")
                            // docker.withRegistry('https://harbor.niyov.com', "harbor.niyov-credentials") {
                                // dockerImage.push()
                        }
                    }
                }
            }
        }

        stage('Build Client & Push To Registry') {
            steps {
                container('docker') {
                    dir('api') {
                        script {
                            sh 'ls -la'
                            sh 'docker build -t natiiyov/todo-list-client:1.0.0 -f Dockerfile.prod .'
                            def dockerImage = docker.image("${DOCKER_IMAGE}")
                            // docker.withRegistry('https://harbor.niyov.com', "harbor.niyov-credentials") {
                                // dockerImage.push()
                        }
                    }
                }
            }
        }

        // stage('Update Deployment File') {
        //     steps {
        //         script {
                    
        //         }
        //     }
        // }
        
    }
}