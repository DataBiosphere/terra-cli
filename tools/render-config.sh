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

mkdir -p rendered

echo "Reading the CI service account key file from Vault"
docker run --rm -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
            vault read -format json ${CI_SA_VAULT_PATH} \
            | jq -r .data > rendered/ci-account.json

echo "Reading the domain-wide delegated test users service account key file from Vault"
docker run --rm -e VAULT_TOKEN=$VAULT_TOKEN ${DSDE_TOOLBOX_DOCKER_IMAGE} \
            vault read -format json ${TEST_USER_SA_VAULT_PATH} \
            | jq -r .data > rendered/test-user-account.json
