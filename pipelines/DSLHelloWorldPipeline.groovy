// Uses Declarative syntax to run commands inside a container.
pipelineJob('Test-Pipeline') {
    // TODO: remove sandbox
    sandbox(boolean sandbox = true)

    definition {
        cps {
            script(readFileFromWorkspace("./pipelines/HelloWorldPipeline.groovy"))
        }
    }
}