// Uses Declarative syntax to run commands inside a container.

def jobName = BRANCH == 'main' ? 'Todo-List-Chart-CICD' : 'Testing/Todo-List-Chart-CICD'

pipelineJob(jobName) {
    triggers {
        genericTrigger {
            token('Todo-List-CICD')
            genericVariables {
                genericVariable {
                    key("release_tag")
                    value("\$.tag_name")
                }
            }
            regexpFilterText("\$action")
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