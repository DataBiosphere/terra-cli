#!/bin/bash

# Initialize a default Cromwell config. Don't overwrite in case the user has
# customized their Cromwell config file on their PD.
cromwell_config=/home/jupyter/cromwell/cromwell.conf
echo "WORKSPACE_BUCKET: $1"
echo "GOOGLE_PROJECT: $2"
if [ ! -f "${cromwell_config}" ]; then
  cat <<EOF | sudo -E tee "${cromwell_config}"
include required(classpath("application"))

google {
  application-name = "cromwell"
  auths = [{
    name = "application_default"
    scheme = "application_default"
  }]
}

backend {
  default = "PAPIv2-beta"
  providers {

    # Disables the Local backend
    Local.config.root = "/dev/null"

    PAPIv2-beta {
      actor-factory = "cromwell.backend.google.pipelines.v2beta.PipelinesApiLifecycleActorFactory"

      config {
        project = "${GOOGLE_PROJECT}"
        concurrent-job-limit = 10
        root = "${WORKSPACE_BUCKET}/workflows/cromwell-executions"

        virtual-private-cloud {
          network-label-key = "vpc-network-name"
          subnetwork-label-key = "vpc-subnetwork-name"
          auth = "application_default"
        }

        genomics {
          auth = "application_default"
          compute-service-account = "${PET_SA_EMAIL}"
          endpoint-url = "https://lifesciences.googleapis.com/"
          location = "us-central1"
        }

        filesystems {
          gcs {
            auth = "application_default"
          }
        }
      }
    }
  }
}
EOF
fi