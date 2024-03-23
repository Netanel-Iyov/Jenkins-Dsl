// Uses Declarative syntax to run commands inside a container.
pipelineJob('Test-Pipeline') {
    definition {
        cps {
            readFileFromWorkspace("./pipelines/HelloWorldPipeline.groovy")
            sandbox()
        }
    }


}