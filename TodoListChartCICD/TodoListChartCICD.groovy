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
                            container('git') {
                                sh "git config --global --add safe.directory ${pwd()}"
                                def tag = sh returnStdout: true, script: 'git rev-parse HEAD'
                                
                                env.IMAGE_TAG = tag.trim()
                            }
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
                                container('kaniko') {
                                    // define the full image tag
                                    def imageTag = "${API_IMAGE}:${IMAGE_TAG}"

                                    sh """
                                        /kaniko/executor \
                                            --dockerfile=Dockerfile.prod \
                                            --context=\$PWD \
                                            --destination=${imageTag} \
                                            --cache=true \
                                            --snapshot-mode=redo \
                                            --cleanup
                                    """

                                    // optionally save the tag in env
                                    env.API_TAG = "${DOCKER_REGISTRY}/${imageTag}"
                                }
                            }
                        }
                    }
                }
            
                stage("Build Client & Push To Registry") {
                    steps {
                        dir("Todo-list/client") {
                            script {
                                container('kaniko') {
                                    // full image tag
                                    def imageTag = "${CLIENT_IMAGE}:${IMAGE_TAG}"

                                    sh """pwd && ls -la"""

                                    sh """
                                        /kaniko/executor \
                                            --dockerfile=Dockerfile.prod \
                                            --context=\$PWD \
                                            --destination=${imageTag} \
                                            --cache=true \
                                            --snapshot-mode=redo \
                                            --build-arg REACT_APP_API_BASE=${REACT_APP_API_BASE} \
                                            --cleanup
                                    """

                                    // save tag in env for later stages
                                    env.CLIENT_TAG = "${DOCKER_REGISTRY}/${imageTag}"
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
                                git credentialsId: GITHUB_CREDENTIALS_ID, url: 'https://github.com/Netanel-Iyov/ArgoCD-GitOps.git', branch: ARGOCD_GITOPS_BRANCH
                            }
                        }
                    }
                }

                stage('Update Application Values') {
                    steps {
                        dir('Todo-List-Chart') {
                            container('git') {
                                script {
                                    // Read the YAML file into a string
                                    def yamlContent = readFile(APPLICATION_MANIFEST)
                                    
                                    // Replace the image tags using the environment variables
                                    yamlContent = yamlContent.replaceAll(/${DOCKER_REPOSITORY}\/todo-list-client.*:.+/, CLIENT_TAG)
                                    yamlContent = yamlContent.replaceAll(/${DOCKER_REPOSITORY}\/todo-list-api.*:.+/, API_TAG)
                                    
                                    // Write the modified YAML content back to the file
                                    writeFile file: APPLICATION_MANIFEST, text: yamlContent
                                    
                                    echo "${APPLICATION_MANIFEST} file has been updated with new versions..."
                                    sh "cat ${APPLICATION_MANIFEST}"

                                    withCredentials([gitUsernamePassword(credentialsId: GITHUB_CREDENTIALS_ID)]) {
                                        sh """
                                            git config --global --add safe.directory ${pwd()}
                                            git config --global user.name "Jenkins-CI-CD-Pipeline"
                                            git config --global user.email "jenkins@jenkins.niyov.com"
                                            git add .

                                            git commit -m 'Jenkins CI-CD Pipeline: Updating ${APPLICATION_MANIFEST}, for ${REVISION}'
                                            git push --set-upstream origin ${ARGOCD_GITOPS_BRANCH}
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