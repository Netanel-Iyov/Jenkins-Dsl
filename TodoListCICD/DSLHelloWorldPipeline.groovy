// Uses Declarative syntax to run commands inside a container.
pipelineJob('Todo-List-CICD') {
    definition {
        cps {
            script(readFileFromWorkspace("./TodoListCICD/TodoLostCICD.groovy"))
        }
    }
}