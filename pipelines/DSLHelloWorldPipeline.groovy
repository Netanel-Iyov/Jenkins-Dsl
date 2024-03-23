// Uses Declarative syntax to run commands inside a container.
pipelineJob('my-pipeline') {
    definition {
        cps {
            readFileFromWorkspace("${WORKSPACE}/pipelines/HelloWorldPipeline.groovy")
            sandbox()
        }
    }


}