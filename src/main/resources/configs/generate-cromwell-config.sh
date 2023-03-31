#!/bin/bash

# Initialize a default Cromwell config. Don't overwrite in case the user has
# customized their Cromwell config file on their PD.
readonly CROMWELL_CONFIG_PATH="$1"
readonly GOOGLE_PROJECT="$2"
readonly PET_SA_EMAIL="$3"
readonly GOOGLE_BUCKET="$4"
readonly CONCURRENT_JOB_LIMIT="$5"

echo "CROMWELL_CONFIG_PATH" : "${CROMWELL_CONFIG_PATH}"
echo "GOOGLE_PROJECT": "${GOOGLE_PROJECT}"
echo "PET_SA_EMAIL": "${PET_SA_EMAIL}"
echo "GOOGLE_BUCKET": "${GOOGLE_BUCKET}"
echo "CONCURRENT_JOB_LIMIT": "${CONCURRENT_JOB_LIMIT}"

if [[ ! -f "${CROMWELL_CONFIG_PATH}" ]]; then
  cat <<EOF | tee "${CROMWELL_CONFIG_PATH}"

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
        concurrent-job-limit = ${CONCURRENT_JOB_LIMIT}
        root = "${GOOGLE_BUCKET}/workflows/cromwell-executions"

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
