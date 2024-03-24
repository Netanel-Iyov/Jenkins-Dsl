// Uses Declarative syntax to run commands inside a container.
pipelineJob('Test-Pipeline') {
    definition {
        cps {
            script(readFileFromWorkspace("./pipelines/HelloWorldPipeline.groovy"))
        }
    }
}