
pipelineJob("cvp-rhel7-rsyslog-image-tests") {
  definition {

    parameters {
      stringParam("CI_MESSAGE", "", "Contents of the CI message received from UMB.")
      stringParam("GIT_REPO", "https://gitlab.sat.engineering.redhat.com/cvp/pipeline.git", "Git repo with the Jenkinsfile.")
      stringParam("GIT_BRANCH", "master", "Git branch to checkout.")
    }

    triggers {
      ciBuildTrigger {
        providerData {
          activeMQSubscriberProviderData {
            // UMB provider name - for development purposes this should be changed to 'Red Hat UMB Stage'.
            // The "Red Hat UMB" provider is configured out-of-the-box by the redhat-ci-plugin
            name("Red Hat UMB")
            overrides {
              // The topic name needs to be unique for every job listening to the UMB (note the UUID in the middle).
              // When reusing (copy-pasting) this configuration, make sure to change the UUID to a different one, or use
              // different unique string
              def uuid = "4ba46bbc-949b-11e8-b83f-54ee754ea14c"
              topic("Consumer.rh-jenkins-ci-plugin.${uuid}.VirtualTopic.eng.ci.redhat-container-image.pipeline.running")
            }
            //
            // Message Checks
            //   See https://datagrepper.engineering.redhat.com/raw?topic=/topic/VirtualTopic.eng.ci.redhat-container-image.pipeline.running&delta=32400
            //   for examples of message content for various images.
            checks {
              msgCheck {
                // The field attribute supports JSONPath notation
                field('$.artifact.type')
                // the expectedValue supports regex.
                expectedValue("cvp")
              }
              msgCheck {
                field('$.artifact.nvr')
                expectedValue("^ubi.*")
              }
            }
          }
        }
        noSquash(true)
      }
    }

    cpsScm {
      scm {
        git {
          remote {
            url('${GIT_REPO}')
          }
          branch('${GIT_BRANCH}')
        }
      }
      scriptPath("Jenkinsfile")
      lightweight(false)
    }
  }
}
