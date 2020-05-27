/**
 * This file is to implement the CVP testing of rhel7/etcd in the "extras" way.
 * Listening build UMB messages -> trigger the test job -> running test -> post test result via UMB message.
 */

def ciMessage = params.CI_MESSAGE // UMB message which triggered the build
def buildMetadata = [:]           // image build metadata; parsed from the UMB message
def result_flag = 0               // flag for testing result, used for the post sending UMB
def status = ''                   // testing result for post sending UMB

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
          echo "Simple Build"
        }
      }
    }

    stage("Run image tests") {
      steps {
        script {
          echo "---------------------- TEST START ---------------------"

          try {
            sh """
               cd /home/cloud-user/containers-ansible/containers-ansible
               ansible-playbook etcd.yml -e image_fullname=registry.access.redhat.com/rhel7/etcd
            """
          }
          catch (exc) {
            result_flag = 1
          }
        }
      }
    }
  }
}
