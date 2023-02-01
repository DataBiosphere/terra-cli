#!/bin/bash
## IMPORTANT --> ADDED FILE ONLY FOR TESTING, DO NOT MERGE TO MASTER

## Usage: ./tools/render-config.sh

## The script assumes that it is being run from the top-level directory "terra-tool-cli/".
## The submodule directory must exist; see https://github.com/verily-src/terra-tool-cli#initializing-submodule

if [ $(basename $PWD) != 'terra-cli' ]; then
  echo "Script must be run from top-level directory 'terra-tool-cli/'"
  exit 1
fi

config_dir="./rendered/verily"
mkdir -p "$config_dir"

#### verily-src: Not needed for Verily deployment; Verily deployment will not have releases.
# used for publishing Docker images to GCR in the terra-cli-dev project

# used for generating domain-wide delegated credentials for test users
echo "Reading the domain-wide delegated test users service account key file"
gcloud secrets versions access "latest" --secret="terra-cli-user-delegated-sa" --project=terra-devel > "${config_dir}/test-user-account.json"

#### verily-src: From GHA, we could use ${{ secrets.GCP_SA_KEY }}
#### But this script is also run locally. So fetch secret from GCP.
# used for creating external cloud resources in the verily-bvdp-cli-test project for tests
echo "Reading the external project service account key file"
gcloud secrets versions access "latest" --secret="terra-cli-test-sa" --project=terra-devel > "${config_dir}/external-project-account.json"

# Read test user refresh tokens
echo "Reading test user refresh tokens"
gcloud secrets versions access "latest" --secret="terra-cli-test-user-01" --project=terra-devel > "${config_dir}/cli_testuser_01@test.verily-bvdp.com.json"
gcloud secrets versions access "latest" --secret="terra-cli-test-user-02" --project=terra-devel > "${config_dir}/cli_testuser_02@test.verily-bvdp.com.json"
gcloud secrets versions access "latest" --secret="terra-cli-test-user-03" --project=terra-devel > "${config_dir}/cli_testuser_03@test.verily-bvdp.com.json"
gcloud secrets versions access "latest" --secret="terra-cli-test-user-04" --project=terra-devel > "${config_dir}/cli_testuser_04@test.verily-bvdp.com.json"
gcloud secrets versions access "latest" --secret="terra-cli-test-user-05" --project=terra-devel > "${config_dir}/cli_testuser_05@test.verily-bvdp.com.json"
gcloud secrets versions access "latest" --secret="terra-cli-test-user-06" --project=terra-devel > "${config_dir}/cli_testuser_06@test.verily-bvdp.com.json"
