def call(String varsFile) {
    def yamlFile = readYaml file: varsFile
    def envVarsToSet = yamlFile['common'] + yamlFile[env.RELEASE_ENVIRONMENT]

    envVarsToSet.each { key, value ->
        env[key] = value
    }

    // Verify tag version
    if (env.RELEASE_ENVIRONMENT == 'staging' || env.RELEASE_ENVIRONMENT == 'production') {
        def pattern = ~/^v\d+\.\d+\.\d+$/
        def validVersion = env.RELEASE_TAG ==~ pattern
        if (!validVersion)
            error "RELEASE_TAG: ${RELEASE_TAG} is Not a Valid Version! Please fix the release/prerelease in Github."
        env.IMAGE_TAG = env.RELEASE_TAG.substring(1)
    }
}

return this