// Uses Declarative syntax to run commands inside a container.
pipelineJob('Update-DNS-Record') {
    definition {
        cps {
            script(readFileFromWorkspace("./UpdateDNSRecords/UpdateDNSRecords.groovy"))
        }
    }
}