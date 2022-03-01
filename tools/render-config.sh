#!/bin/bash

## This script renders configuration files needed for development and CI/CD.
## Dependencies: vault
## Inputs: VAULT_TOKEN (arg, optional) default is $HOME/.vault-token
## Usage: ./tools/render-config.sh

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [ $(basename $PWD) != 'terra-cli' ]; then
  echo "Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

VAULT_TOKEN=${1:-$(cat $HOME/.vault-token)}
DSDE_TOOLBOX_DOCKER_IMAGE=broadinstitute/dsde-toolbox:consul-0.20.0
CI_SA_VAULT_PATH=secret/dsde/terra/kernel/dev/common/ci/ci-account.json
TEST_USER_SA_VAULT_PATH=secret/dsde/firecloud/dev/common/firecloud-account.json
TEST_USERS_VAULT_PATH=secret/dsde/terra/cli-test/test-users
EXT_PROJECT_SA_VAULT_PATH=secret/dsde/terra/cli-test/default/service-account-admin.json
JANITOR_CLIENT_SA_VAULT_PATH=secret/dsde/terra/kernel/integration/tools/crl_janitor/client-sa
VERILYCLI_WSM_SA_VAULT_PATH=secret/dsde/terra/kernel/integration/verilycli/workspace/app-sa

# Helper function to read a secret from Vault and write it to a local file in the rendered/broad/ directory.
# Inputs: vault path, file name, [optional] decode from base 64
# Usage: readFromVault $CI_SA_VAULT_PATH ci-account.json
#        readFromValue $JANITOR_CLIENT_SA_VAULT_PATH janitor-client.json base64
readFromVault () {
  vaultPath=$1
  fileName=$2
  decodeBase64=$3
  if [ -z "$vaultPath" ] || [ -z "$fileName" ]; then
    echo "Two arguments required for readFromVault function"
    exit 1
  fi
  if [ -z "$decodeBase64" ]; then
    docker run --rm -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
              vault read -format json $vaultPath \
              | jq -r .data > "rendered/broad/$fileName"
  else
    docker run --rm -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
              vault read -format json $vaultPath \
              | jq -r .data.key | base64 -d > "rendered/broad/$fileName"
  fi
  return 0
}

mkdir -p rendered/broad

# used for publishing Docker images to GCR in the terra-cli-dev project
echo "Reading the CI service account key file from Vault"
readFromVault "$CI_SA_VAULT_PATH" "ci-account.json"

# used for generating domain-wide delegated credentials for test users
echo "Reading the domain-wide delegated test users service account key file from Vault"
readFromVault "$TEST_USER_SA_VAULT_PATH" "test-user-account.json"

# used for creating external cloud resources in the terra-cli-test project for tests
echo "Reading the external project service account key file from Vault"
readFromVault "$EXT_PROJECT_SA_VAULT_PATH" "external-project-account.json"

# used for cleaning up external (to WSM) test resources with Janitor
echo "Reading the Janitor client service account key file from Vault"
readFromVault "$JANITOR_CLIENT_SA_VAULT_PATH" "janitor-client.json" "base64"

# used for granting break-glass access to a workspace in the verilycli deployment
echo "Reading the WSM app service account key file for the verilycli deployment from Vault"
readFromVault "$VERILYCLI_WSM_SA_VAULT_PATH" "verilycli-wsm-sa.json" "base64"

# Read test user refresh tokens
echo "Reading test user refresh tokens from Vault"
testUsers=$(cat src/test/resources/testconfigs/broad.json | jq -r '.testUsers[] | {email} | join (" ")')
while IFS= read -r line; do
  readFromVault "$TEST_USERS_VAULT_PATH/${line}" "${line}.json"
done <<< "$testUsers"
