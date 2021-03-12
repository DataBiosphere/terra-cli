#!/bin/bash

## This script renders configuration files needed for development and CI/CD.
## Dependencies: vault
## Usage: ./tools/render-config.sh

## The script assumes that it is being run from the top-level directory "terra-cli/".

echo "Reading the CI service account key file from Vault"
mkdir -p rendered
vault read -format json secret/dsde/terra/kernel/dev/common/ci/ci-account.json | jq .data > rendered/ci-account.json
