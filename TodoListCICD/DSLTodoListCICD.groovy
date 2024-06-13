// Uses Declarative syntax to run commands inside a container.
pipelineJob('Todo-List-CICD') {
    triggers {
        genericTrigger {
            token('test-token')
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
            regexpFilterExpression("refs/heads/test-CICD")
        }

    }

    definition {
        cps {
            script(readFileFromWorkspace("./TodoListCICD/TodoListCICD.groovy"))
        }
    }
}