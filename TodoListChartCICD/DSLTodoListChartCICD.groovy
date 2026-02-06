// Uses Declarative syntax to run commands inside a container.
def jobName = 'Todo-List-Chart-CICD'

pipelineJob(jobName) {
    if (PRODUCTION_ENV == 'true') { // This variable is coming from the seed job Additional Parameters
        triggers {
            genericTrigger {
                token('Todo-List-CICD')
                genericVariables {
                    genericVariable {
                        key("ACTION")
                        value("\$.action")
                    }
                    genericVariable {
                        key("RELEASE_TAG")
                        value("\$.release.tag_name")
                    }
                }
                regexpFilterText("\$ACTION")
                regexpFilterExpression("released|prereleased")
            }
        }
    }

    parameters {
        stringParam('BRANCH', '', 'Branch to deploy to testing environment (only for testing, other environments are triggered via Github webhooks)')
        stringParam('ACTION', '', 'Leave blank for testing environment (gets filled from github webhook)')
        stringParam('RELEASE_TAG', '', 'Leave blank for testing environment (gets filled from github webhook)')
    }

    definition {
        cpsScm {
            scm {
                git {
                    branch BRANCH // This variable is coming from the seed job Additional Parameters
                    remote {
                        url 'https://github.com/Netanel-Iyov/Jenkins-Dsl.git'
                        credentials('GitHub-Credentials')
                    }
                    extensions {
                        wipeOutWorkspace()
                    }
                }
            }
            scriptPath('TodoListChartCICD/TodoListChartCICD.groovy')
        }
    }
}
