import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import groovy.json.JsonSlurper

def applicationValues = [
    'api' : [
            'workdir': 'api',
            'versionFile': './api/metadata.json',
            'changedFilesPrefix': 'api/',
            'imageName': 'natiiyov/todo-list-api',
            'deploymentFile': 'api/deployment.yaml',
            'imagePattern': ~/natiiyov\/todo-list-api:\d+\.\d+\.\d+/
            ], 
    'client' : [
                'workdir': 'client',
                'versionFile': './client/metadata.json',
                'changedFilesPrefix': 'client/',
                'imageName': 'natiiyov/todo-list-client',
                'deploymentFile': 'client/deployment.yaml',
                'imagePattern': ~/natiiyov\/todo-list-client:\d+\.\d+\.\d+/
            ]
]

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
                container('docker') {
                    dir('Todo-list') {
                        script {
                            List allFilesChanged = []
                            List jsonLists = [ADDED_FILES, REMOVED_FILES, MODIFIED_FILES]
                            JsonSlurper jsonSlurper = new JsonSlurper()
                            
                            jsonLists.each{ jsonList ->
                                allFilesChanged.addAll(jsonSlurper.parseText(jsonList))
                            }
                            
                            echo "Changed Files: ${allFilesChanged}"
                            
                            applicationValues.each { appLabel, appData ->
                                def jsonFile = readFile(file: appData.versionFile)
                                def jsonData = readJSON(text: jsonFile)
    
                                def version = jsonData.version
                                def changedFilesMatched = false

                                // check if changed files match 
                                allFilesChanged.each { file -> 
                                    changedFilesMatched = file.startsWith(appData.changedFilesPrefix) ? true : changedFilesMatched
                                }
                                def imageTag = "${appData.imageName}:${version}"
                                
                                withCredentials([usernamePassword(credentialsId: 'DockerHub-Credentials', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                                    def isImageExist = sh(script: "docker login -u ${USERNAME} -p ${PASSWORD} && docker manifest inspect ${imageTag} > /dev/null", returnStatus: true) == 0 ? true : false
                                
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
            }
        }

        // stage('Test') {

        // }

        stage("Build API & Push To Registry") {
            steps {
                dir("Todo-list/${applicationValues.api.workdir}") {
                    script {
                        container('docker') {
                            sh 'pwd && ls -la'
                            if (applicationValues.api.toDeploy) {
                                docker.withRegistry('https://registry.hub.docker.com', 'DockerHub-Credentials') {
                                    def dockerImage = docker.build(applicationValues.api.tag, "-f Dockerfile.prod .")
                                    // dockerImage.push()
                                }
                            } else {
                                Utils.markStageSkippedForConditional(STAGE_NAME)
                            }
                        }
                    }
                }
            }
        }

        stage("Build Client & Push To Registry") {
            steps {
                dir("Todo-list/${applicationValues.client.workdir}") {
                    script {
                        if (applicationValues.client.toDeploy) {
                            container('docker') {
                                docker.withRegistry('https://registry.hub.docker.com', 'DockerHub-Credentials') {
                                    def dockerImage = docker.build(applicationValues.client.tag, "-f Dockerfile.prod .")
                                    // dockerImage.push()
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
                            def newDeploymentFileContent = writeYaml file: appData.deploymentFile, data: deploymentFileData, overwrite: true

                            echo "${appLabel} Deployment file has been updated"
                            sh "cat ${appData.deploymentFile}"
                        }
                    }

                    // commit and push
                }
            }
        }
    }
}