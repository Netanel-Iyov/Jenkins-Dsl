// Uses Declarative syntax to run commands inside a container.
pipelineJob('Test-Pipeline') {
    // TODO: remove sandbox

    definition {
        cps {
            script(readFileFromWorkspace("./pipelines/HelloWorldPipeline.groovy"))
        }
    }
}