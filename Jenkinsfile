/**
 * This file is to implement the CVP testing of rhel7/rsyslog in the "extras" way.
 * Listening build UMB messages -> trigger the test job -> running test -> post test result via UMB message.
 */

def ciMessage = params.CI_MESSAGE // UMB message which triggered the build
def buildMetadata = [:]           // image build metadata; parsed from the UMB message
def result_flag = 0               // flag for testing result, used for the post sending UMB
def status = ''                   // testing result for post sending UMB


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
        }
      }
    }

    stage("Run image tests") {
      steps {
        script {
          echo "---------------------- TEST START ---------------------"

          def dmg_ns = buildMetadata['namespace']
          def img_name = buildMetadata['name']
          def img_tag = buildMetadata['image_tag']
          def img_fn = buildMetadata['full_name']

          try {
            sh """
               cd /home/cloud-user/containers-ansible/containers-ansible
               ansible-playbook rsyslog.yml -e image_version=/${dmg_ns}/${img_name}:${img_tag}
            """
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
            def namespace = "atomic-rsyslog-rhel7-container-test"
            def type = "default"
            def testName = "cvetest"
            if (result_flag == 0) {
              status = "PASSED"
            } else {
              status = "FAILED"
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
                    "email": "weshen@redhat.com",
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
