// Uses Declarative syntax to run commands inside a container.
def jobName = 'Todo-List-Chart-CICD'
def fullJobName = PRODUCTION_ENV == 'true' ? jobName : "${JOBS_BASE_PATH}/${jobName}"
pipelineJob(fullJobName) {
    if (PRODUCTION_ENV == 'true') {
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
                    branch BRANCH
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