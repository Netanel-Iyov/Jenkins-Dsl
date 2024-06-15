// Uses Declarative syntax to run commands inside a container.
pipelineJob('Todo-List-CICD') {
    triggers {
        genericTrigger {
            token('Todo-List-CICD')
            genericVariables {
                genericVariable {
                    key("ADDED_FILES")
                    value("\$.head_commit.added")
                }
                genericVariable {
                    key("REMOVED_FILES")
                    value("\$.head_commit.removed")
                }
                genericVariable {
                    key("MODIFIED_FILES")
                    value("\$.head_commit.modified")
                }
                genericVariable {
                    key("REF")
                    value("\$.ref")
                }
            }
            regexpFilterText("\$REF")
            regexpFilterExpression("refs/heads/master")
        }
    }

    definition {
        cps {
            script(readFileFromWorkspace("./TodoListCICD/TodoListCICD.groovy"))
            sandbox()
        }
    }
}