// Uses Declarative syntax to run commands inside a container.
pipelineJob('Todo-List-CICD') {
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