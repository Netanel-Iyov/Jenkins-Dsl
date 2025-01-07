// Uses Declarative syntax to run commands inside a container.
def jobName = 'Todo-List-Chart-CICD'
def fullJobName = PRODUCTION_ENV == 'true' ? jobName : "${JOBS_BASE_PATH}/${jobName}"
pipelineJob(fullJobName) {
    triggers {
        genericTrigger {
            token('Todo-List-CICD')
            genericVariables {
                genericVariable {
                    key("ACTION")
                    value("\$.release.action")
                }
                genericVariable {
                    key("RELEASE_TAG")
                    value("\$.tag_name")
                }
            }
            regexpFilterText("\$ACTION")
            regexpFilterExpression("prereleased|released")
        }
    }

    definition {
        cps {
            script(readFileFromWorkspace("./TodoListChartCICD/TodoListChartCICD.groovy"))
            sandbox()
        }
    }
}