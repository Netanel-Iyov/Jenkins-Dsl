// Uses Declarative syntax to run commands inside a container.
pipelineJob('Test-Pipeline') {
    definition {
        cps {
            readFileFromWorkspace("${WORKSPACE}/pipelines/HelloWorldPipeline.groovy")
            sandbox()
        }
    }


}