#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

## This script renders configuration files needed for development and CI/CD.
## Dependencies: vault
## Inputs: VAULT_TOKEN (arg, optional) default is $HOME/.vault-token
## Usage: ./tools/render-config.sh

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [[ $(basename "${PWD}") != 'terra-cli' ]]; then
  >&2 echo "ERROR: Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

VAULT_TOKEN=${1:-$(cat "${HOME}"/.vault-token)}
DSDE_TOOLBOX_DOCKER_IMAGE=broadinstitute/dsde-toolbox:dev
CI_SA_VAULT_PATH=secret/dsde/terra/kernel/dev/common/ci/ci-account.json
TEST_USER_SA_VAULT_PATH=secret/dsde/firecloud/dev/common/firecloud-account.json
TEST_USERS_VAULT_PATH=secret/dsde/terra/cli-test/test-users
EXT_PROJECT_SA_VAULT_PATH=secret/dsde/terra/cli-test/default/service-account-admin.json
JANITOR_CLIENT_SA_VAULT_PATH=secret/dsde/terra/kernel/integration/tools/crl_janitor/client-sa
VERILYCLI_WSM_SA_VAULT_PATH=secret/dsde/terra/kernel/integration/verilycli/workspace/app-sa
CLIENT_CRED_VAULT_PATH=secret/dsde/terra/cli/oauth-client-credentials

# Helper function to read a secret from Vault and write it to a local file in the rendered/ directory.
# Inputs: vault path, file name, [optional] decode from base 64
# Usage: readFromVault $CI_SA_VAULT_PATH ci-account.json
#        readFromValue $JANITOR_CLIENT_SA_VAULT_PATH janitor-client.json base64
readFromVault () {
  vaultPath="$1"
  fileName="$2"
  decodeBase64="${3:-}" # empty string if $3 not set
  if [[ -z "${vaultPath}" ]] || [[ -z "${fileName}" ]]; then
    >&2 echo "ERROR: Two arguments required for readFromVault function"
    exit 1
  fi
  if [[ -z "${decodeBase64}" ]]; then
    docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
              vault read -format json "${vaultPath}" \
              | jq -r .data > "rendered/${fileName}"
  else
    docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
              vault read -format json "${vaultPath}" \
              | jq -r .data.key | base64 -d > "rendered/${fileName}"
  fi
  return 0
}

mkdir -p rendered

# used for publishing Docker images to GCR in the terra-cli-dev project
echo "Reading the CI service account key file from Vault"
readFromVault "${CI_SA_VAULT_PATH}" "ci-account.json"

# used for generating domain-wide delegated credentials for test users
echo "Reading the domain-wide delegated test users service account key file from Vault"
readFromVault "${TEST_USER_SA_VAULT_PATH}" "test-user-account.json"

# used for creating external cloud resources in the terra-cli-test project for tests
echo "Reading the external project service account key file from Vault"
readFromVault "${EXT_PROJECT_SA_VAULT_PATH}" "external-project-account.json"

# used for cleaning up external (to WSM) test resources with Janitor
echo "Reading the Janitor client service account key file from Vault"
readFromVault "${JANITOR_CLIENT_SA_VAULT_PATH}" "janitor-client.json" "base64"

# used for granting break-glass access to a workspace in the verilycli deployment
echo "Reading the WSM app service account key file for the verilycli deployment from Vault"
readFromVault "${VERILYCLI_WSM_SA_VAULT_PATH}" "verilycli-wsm-sa.json" "base64"

# Read test user refresh tokens
echo "Reading test user refresh tokens from Vault"
testUsers=$(cat "src/test/resources/testconfigs/broad.json" | jq -r '.testUsers[] | {email} | join (" ")')
while IFS= read -r line; do
  readFromVault "${TEST_USERS_VAULT_PATH}/${line}" "${line}.json"
done <<< "${testUsers}"

echo "Fetching Broad client id and client secrets"
clientId=$(docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
            vault read -format json "${CLIENT_CRED_VAULT_PATH}" | \
            jq -r '.data."broad-client-id"')
clientSecret=$(docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
            vault read -format json "${CLIENT_CRED_VAULT_PATH}" | \
            jq -r '.data."broad-client-secret"')
./tools/client-credentials.sh "src/main/resources/broad_secret.json" "${clientId}" "${clientSecret}" \
                              "rendered/broad_secret.json"

echo "Fetching Verily client id and client secrets"
clientId=$(docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
            vault read -format json "${CLIENT_CRED_VAULT_PATH}" | \
            jq -r '.data."verily-client-id"')
clientSecret=$(docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
            vault read -format json "${CLIENT_CRED_VAULT_PATH}" | \
            jq -r '.data."verily-client-secret"')
./tools/client-credentials.sh "src/main/resources/verily_secret.json" "${clientId}" "${clientSecret}" \
                              "rendered/verily_secret.json"

echo "Fetching Verily auth0 dev client id and client secrets"
clientId=$(docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
            vault read -format json "${CLIENT_CRED_VAULT_PATH}" | \
            jq -r '.data."verily-auth0-dev-client-id"')
clientSecret=$(docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
            vault read -format json "${CLIENT_CRED_VAULT_PATH}" | \
            jq -r '.data."verily-auth0-dev-client-secret"')
./tools/client-credentials.sh "src/main/resources/verily_auth0_dev_secret.json" "${clientId}" "${clientSecret}" \
                              "rendered/verily_auth0_dev_secret.json"

echo "Fetching Verily auth0 prod client id and client secrets"
clientId=$(docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
            vault read -format json "${CLIENT_CRED_VAULT_PATH}" | \
            jq -r '.data."verily-auth0-prod-client-id"')
clientSecret=$(docker run --rm -e VAULT_TOKEN="${VAULT_TOKEN}" "${DSDE_TOOLBOX_DOCKER_IMAGE}" \
            vault read -format json "${CLIENT_CRED_VAULT_PATH}" | \
            jq -r '.data."verily-auth0-prod-client-secret"')
./tools/client-credentials.sh "src/main/resources/verily_auth0_prod_secret.json" "${clientId}" "${clientSecret}" \
                              "rendered/verily_auth0_prod_secret.json"
