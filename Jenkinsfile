/**
 * This file showing the interaction with Unified Message Bus (UMB).
 */

def ciMessage = params.CI_MESSAGE // UMB message which triggered the build
def buildMetadata = [:]           // image build metadata; parsed from the UMB message


// Load the contra-int-lib library which will be used for UMB message parsing
library identifier: "contra-int-lib@master",
        retriever: modernSCM([$class: 'GitSCMSource',
                              remote: "https://gitlab.sat.engineering.redhat.com/contra/contra-int-lib.git"])

pipeline {
  agent {
    // Ideally, jobs should _not_ run directly on a Jenkins master, but rather on a different agent (slave)
    // This sample is simple enough though and running on master requires less configuration.
    label("rhel7-main")
  }

  stages {
    stage("Parse 'redhat-container-image.pipeline.running' message") {
      steps {
        script {
          echo "Raw message:\n${ciMessage}"

          buildMetadata = extractCVPPipelineRunningMessageData(ciMessage)

          def metadataStr = buildMetadata
              .collect { meta -> "\t${meta.key} -> ${meta.value}"}
              .join("\n")
          echo "Build metadata:\n${metadataStr}"

          def img_ns = buildMetadata['namespace']
          def img_name = buildMetadata['name']
          def img_tag = buildMetadata['image_tag']
          def img_fn = buildMetadata['full_name']
        }
      }
    }

    stage("Run image tests") {
      steps {
        script {
          echo "---------------------- TEST START ---------------------"
          def result_flag = 0
          sh 'cd containers-ansible/containers-ansible'
          sh 'docker pull ${img_fn}'
          try {
            sh 'ansible-playbook rsyslog.yml -e image_version=/rhel7/rsyslog'
          }
          catch (exc) {
            result_flag = 1
          }
        }
      }
      post {
        always {
          script {
            // report test results to ResultsDB
            def provider = "Red Hat UMB" // change the provider to "Red Hat UMB Stage" for development purposes

            // the following three values need to match the configuration in gating.yaml
            def namespace = "atomic-rsyslog-container-test"
            def type = "default"
            def testName = "fulltest"
            if (result_flag == 0) {
              def status = "PASSED"
            } else {
              def status = "FAILED"
            }

            def brewTaskID = buildMetadata['id']
            def brewNvr = buildMetadata['nvr']
            def product = buildMetadata['component']

            def msgContent = """
             {
                "category": "${testName}",
                "status": "${status}",
                "ci": {
                    "url": "https://jenkins-cvp-ci.cloud.paas.upshift.redhat.com/",
                    "team": "atomic-qe",
                    "email": "atomic-qe@redhat.com",
                    "name": "Edward Shen"
                },
                "run": {
                    "url": "${BUILD_URL}",
                    "log": "${BUILD_URL}/console"
                },
                "system": {
                    "provider": "openshift",
                    "os": "openshift"
                },
                "artifact": {
                    "nvr": "${brewNvr}",
                    "component": "${product}",
                    "type": "brew-build",
                    "id": "${brewTaskID}",
                    "issuer": "Unknown issuer"
                },
                "type": "${type}",
                "namespace": "${namespace}"
              }"""

            echo "Sending the following message to UMB:\n${msgContent}"

            sendCIMessage(messageContent: msgContent,
                messageProperties: '',
                messageType: 'Custom',
                overrides: [topic: "VirtualTopic.eng.ci.brew-build.test.complete"],
                providerName: provider)
          }
        }
      }
    }
  }
}
