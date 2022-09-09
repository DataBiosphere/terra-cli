#!/bin/bash

# Initialize a default Cromwell config. Don't overwrite in case the user has
# customized their Cromwell config file on their PD.
echo "cromwell_config" : $1
echo "GOOGLE_PROJECT": $2
echo "WORKSPACE_BUCKET": $3
echo "PET_SA_EMAIL": $4
if [ ! -f "$1" ]; then
  cat <<EOF | tee "$1"

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
        project = "$2"
        concurrent-job-limit = 10
        root = "$3/workflows/cromwell-executions"

        virtual-private-cloud {
          network-label-key = "vpc-network-name"
          subnetwork-label-key = "vpc-subnetwork-name"
          auth = "application_default"
        }

        genomics {
          auth = "application_default"
          compute-service-account = "$4"
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