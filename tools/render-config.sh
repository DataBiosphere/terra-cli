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
EXT_PROJECT_SA_VAULT_PATH=secret/dsde/terra/cli-test/default/service-account-admin.json

# Helper function to read a secret from Vault and write it to a local file in the rendered/ directory.
# Inputs: vault path, file name
# Usage: readFromVault $CI_SA_VAULT_PATH ci-account.json
readFromVault () {
  vaultPath=$1
  fileName=$2
  if [ -z "$vaultPath" ] || [ -z "$fileName" ]; then
    echo "Two arguments required for readFromVault function"
    exit 1
  fi
  docker run --rm -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
            vault read -format json $vaultPath \
            | jq -r .data > "rendered/$fileName"
  return 0
}

mkdir -p rendered

# used for publishing Docker images to GCR in the terra-cli-dev project
echo "Reading the CI service account key file from Vault"
readFromVault "$CI_SA_VAULT_PATH" "ci-account.json"

# used for generating domain-wide delegated credentials for test users
echo "Reading the domain-wide delegated test users service account key file from Vault"
readFromVault "$TEST_USER_SA_VAULT_PATH" "test-user-account.json"

# used for creating external cloud resources in the terra-cli-test project for tests
echo "Reading the external project service account key file from Vault"
readFromVault "$EXT_PROJECT_SA_VAULT_PATH" "external-project-account.json"
