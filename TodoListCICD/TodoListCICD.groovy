import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def applicationValues = [
    'api' : [
            'workdir': 'api',
            'versionFile': './api/metadata.json',
            'changesFilesRegex': 'api/*',
            'imageName': 'natiiyov/todo-list-api',
            'deploymentFile': 'api/deployment.yaml',
            'imagePattern': ~/natiiyov\/todo-list-api:\d+\.\d+\.\d+/
            ], 
    'client' : [
                'workdir': 'client',
                'versionFile': './client/metadata.json',
                'changesFilesRegex': 'client/*',
                'imageName': 'natiiyov/todo-list-client',
                'deploymentFile': 'client/deployment.yaml',
                'imagePattern': ~/natiiyov\/todo-list-client:\d+\.\d+\.\d+/
            ]
]

pipeline {
    agent {
        kubernetes {
            yamlFile 'KubernetesPod.yaml'
            defaultContainer 'shell'
        }
    }

    stages {
        stage('Checkout') {
            steps {
                dir('Todo-list') {
                    cleanWs()
                    git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/Todo-list', branch: 'test-CICD'
                    script {
                        sh 'echo "Display Checkout content" && ls -la'
                    }
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
            steps {
                dir('Todo-list') {
                    script {
                        applicationValues.each { appLabel, appData ->
                            def jsonFile = readFile(file: appData.versionsFile)
                            def jsonData = readJSON(text: jsonFile)

                            def version = jsonData.version

                            // check if changed files match 
                            def changedFilesMatched = true
                            def imageTag = "${appData.imageName}:${version}"
                            def isImageExist = sh("docker manifest inspect ${imageTag} > /dev/null" == 0, returnStatus: true) ? true : false
                            
                            if (changedFilesMatched && !isImageExist) {
                                appData['toDeploy'] = true
                                appData['tag'] = imageTag
                            } else {
                                appData['toDeploy'] = false
                                sh "echo 'Tag ${imageTag} already exist in docker registry. Please make sure you didnt forget to update the version in ${versionFile}'"
                            }
                        }
                    }
                }
            }
        }

        // stage('Test') {

        // }

        stage("Build API & Push To Registry") {
            steps {
                dir('Todo-list') {
                    script {
                        if (applicationValues.api.toDeploy) {
                            container('docker') {
                                dir(appData.workdir) {
                                    docker.withRegistry('https://hub.docker.com', 'DockerHub-Credentials') {
                                        def dockerImage = docker.build(applicationValues.api.tag, "-f Dockerfile.prod .")
                                        // dockerImage.push()
                                    }
                                }
                            }
                        } else {
                            Utils.markStageSkippedForConditional(STAGE_NAME)
                        }
                    }
                }
            }
        }

        stage("Build Client & Push To Registry") {
            steps {
                dir('Todo-list/client') {
                    script {
                        if (applicationValues.client.toDeploy) {
                            container('docker') {
                                dir(appData.workdir) {
                                    docker.withRegistry('https://hub.docker.com', 'DockerHub-Credentials') {
                                        def dockerImage = docker.build(applicationValues.client.tag, "-f Dockerfile.prod .")
                                        // dockerImage.push()
                                    }
                                }
                            }
                        } else {
                            Utils.markStageSkippedForConditional(STAGE_NAME)
                        }
                    }
                }
            }
        } 

        stage('Checkout ArgoCD Repository') {
            steps {
                dir('argocd-todolist') {
                    git credentialsId: 'Github-Credentials', url: 'https://github.com/Netanel-Iyov/todolist-argocd', branch: 'main'
                }
            }
        }

        stage('Update Deployment files') {
            steps {
                dir('argocd-todolist') {
                    script {
                        applicationValues.each { appLabel, appData ->
                            if (!appData.toDeploy) {
                                echo "Skipping ${appLabel} Deployment"
                            }

                            // Read the deployment.yaml file into deploymentFileData
                            def deploymentFileData = readYaml file: appData.deploymentFile
                            
                            // Define the regex pattern for the image
                            def pattern = appData.imagePattern
                            
                            // Iterate over the containers and update the image for the specified container
                            def containers = deploymentFileData.spec.template.spec.containers
                            containers.each { container ->
                                if (container.image ==~ pattern) {
                                    container.image = appData.tag
                                }
                            }
                            
                            // Write the modified deploymentFileData back to the deployment.yaml file
                            def newDeploymentFileContent = writeYaml file: appData.deploymentFile, data: deploymentFileData, overwrite: true, returnText: true

                            echo "${appLabel} Deployment file contents are: ${newDeploymentFileContent}"
                        }
                    }

                    // commit and push
                }
            }
        }
    }
}