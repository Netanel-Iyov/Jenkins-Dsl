pipeline {
    agent {
        kubernetes {
            yamlFile 'TodoListChartCICD/utils/K8SPod.yaml'
            defaultContainer 'shell'
        }
    }

    stages {
        stage('Setup') {
            steps {
                script {
                    def varsFile = 'TodoListChartCICD/utils/vars.yaml'
                    load("TodoListChartCICD/utils/setup.groovy").call(varsFile)
                    sh 'env'
                }
            }
        }

        stage('Checkout') {
            steps {
                dir('Todo-list') {
                    script {
                        checkout scmGit(branches: [[name: REF]], userRemoteConfigs: [[credentialsId: GITHUB_CREDENTIALS_ID, url: 'https://github.com/Netanel-Iyov/Todo-list']])
                        
                        // set the image tag to be the commit hash in case of testing env deployment
                        if (RELEASE_ENVIRONMENT == 'testing') {
                            env.IMAGE_TAG = sh('git rev-parse HEAD', returnStdout: true).trim()
                        }
                    }
                }
            }
        }

        stage('Build') {
            parallel {
                stage("Build API & Push To Registry") {
                    steps {
                        dir("Todo-list/api") {
                            script {
                                container('docker') {
                                    docker.withRegistry(DOCKER_REGISTRY, DOCKER_REGISTRY_CREDENTIALS) {
                                        def imageTag = "${API_IMAGE}:${IMAGE_TAG}"
                                        def dockerImage = docker.build(imageTag, "--no-cache -f Dockerfile.prod .")
                                        dockerImage.push()

                                        env.API_TAG = imageTag
                                    }
                                }
                            }
                        }
                    }
                }
            
                stage("Build Client & Push To Registry") {
                    steps {
                        dir("Todo-list/client") {
                            script {
                                container('docker') {
                                    // Use withCredentials to retrieve the secret file
                                    def imageTag = "${CLIENT_IMAGE}:${IMAGE_TAG}"
                                    docker.withRegistry(DOCKER_REGISTRY, DOCKER_REGISTRY_CREDENTIALS) {
                                        def dockerImage = docker.build(imageTag, "--no-cache --build-arg REACT_APP_API_BASE=${REACT_APP_API_BASE} -f Dockerfile.prod .")
                                        dockerImage.push()

                                        env.CLIENT_TAG = imageTag
                                    }
                                }
                            }
                        }
                    }
                } 
            }
        }

        stage('Deploy') {
            stages {
                stage('Checkout Todo-List Chart Repository') {
                    steps {
                        container('git') {
                            dir('Todo-List-Chart') {
                                git credentialsId: GITHUB_CREDENTIALS_ID, url: 'https://github.com/Netanel-Iyov/Todo-List-Chart.git', branch: HELM_CHART_BRANCH
                            }
                        }
                    }
                }

                stage('Update Helm Chart Values') {
                    steps {
                        dir('Todo-List-Chart') {
                            container('git') {
                                script {
                                    // Read the YAML file into a string
                                    def yamlContent = readFile(HELM_CHART_VALUES_FILE)
                                    
                                    // Replace the image tags using the environment variables
                                    yamlContent = yamlContent.replaceAll(/${DOCKER_REPOSITORY}\/todo-list-client.*:\d+\.\d+\.\d+/, CLIENT_TAG)
                                    yamlContent = yamlContent.replaceAll(/${DOCKER_REPOSITORY}\/todo-list-api.*:\d+\.\d+\.\d+/, API_TAG)
                                    
                                    // Write the modified YAML content back to the file
                                    writeFile file: HELM_CHART_VALUES_FILE, text: yamlContent
                                    
                                    echo "${HELM_CHART_VALUES_FILE} file has been updated with new versions..."
                                    sh "cat ${HELM_CHART_VALUES_FILE}"

                                    withCredentials([gitUsernamePassword(credentialsId: GITHUB_CREDENTIALS_ID)]) {
                                        sh """
                                            git config --global --add safe.directory ${pwd()}
                                            git config --global user.name "Jenkins-CI-CD-Pipeline"
                                            git config --global user.email "jenkins@jenkins.niyov.com"
                                            git add .

                                            git commit -m 'Jenkins CI-CD Pipeline: Updating ${HELM_CHART_VALUES_FILE}, for ${REVISION}'
                                            git push --set-upstream origin ${HELM_CHART_BRANCH}
                                        """
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}