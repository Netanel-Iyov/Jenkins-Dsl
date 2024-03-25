// pipeline {
//     agent {
//         kubernetes {
//             inheritFrom 'jnlp-pod-agent'
//             defaultContainer 'jnlp'
//         }
//     }

//     stages {
//         stage('Build') {
//             steps {
//                 sh 'echo "Hello World"'
//             }
//         }
//     }
// }
pipeline {
    agent none

    podTemplate(containers: [
        containerTemplate(name: 'jnlp', image: 'jenkins/inbound-agent:4.13.3-1', args: '${computer.jnlpmac} ${computer.name}')
    ]) {
        stages {
            node(POD_LABEL) {
                stage('Test') {
                    steps {
                        container('jnlp') {
                            sh 'echo "Hello World"'
                            sh "echo ${POD_LABEL}"
                        }
                    }
                }
            }
        }
    }
}