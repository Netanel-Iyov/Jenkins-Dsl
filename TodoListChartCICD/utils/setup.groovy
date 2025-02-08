def call(String varsFile) {
    switch(params.ACTION) {
        case 'released':
            env.RELEASE_ENVIRONMENT = 'production'
            currentBuild.displayName = "#${BUILD_NUMBER} - Production - ${params.RELEASE_TAG}"
            env.REVISION = params.RELEASE_TAG
            env.REF = "refs/tags/${params.RELEASE_TAG}"
            break
        case 'prereleased':
            env.RELEASE_ENVIRONMENT = 'staging' 
            currentBuild.displayName = "#${BUILD_NUMBER} - Staging - ${params.RELEASE_TAG}"
            env.REVISION = params.RELEASE_TAG
            env.REF = "refs/tags/${params.RELEASE_TAG}"
            break
        case null:
            env.RELEASE_ENVIRONMENT = 'testing'
            currentBuild.displayName = "#${BUILD_NUMBER} - Testing - ${params.BRANCH}"
            env.REVISION = params.BRANCH
            env.REF = "refs/heads/${params.BRANCH}"
            break
    }
    
    // read Env vars from a configuration file
    def yamlFile = readYaml file: varsFile
    def envVarsToSet = yamlFile['common'] + yamlFile[env.RELEASE_ENVIRONMENT]

    envVarsToSet.each { key, value ->
        env[key] = value
    }

    // Validate tag version for staging and release environments
    if (env.RELEASE_ENVIRONMENT == 'staging' || env.RELEASE_ENVIRONMENT == 'production') {
        def pattern = ~/^v\d+\.\d+\.\d+$/
        def validVersion = params.RELEASE_TAG ==~ pattern
        if (!validVersion)
            error "RELEASE_TAG: ${RELEASE_TAG} is Not a Valid Version! Please fix the release/prerelease in Github."
        env.IMAGE_TAG = params.RELEASE_TAG.substring(1)
    }
}

return this