#!/bin/bash
set -o errexit
set -o nounset
set -o pipefail

## This script adds client id and secrets into relevant files
#
## Dependencies: jq
## Inputs: secretsFile (arg, required) path to the secrets file (valid format)
##         renderedSecretsFile (arg, required) path to rendered secrets file.
##         clientId (arg, required) client id
##         clientSecret (arg, required) client secret
## Usage: ./client-credentials.sh <file> <renderedFile> <id> <secret> --> rendered client_id & client_secret to file saving it as renderedFile

readonly secretsFile="$1"
readonly clientId="$2"
readonly clientSecret="$3"
readonly renderedSecretsFile="${4:-$1}"

if [[ ! -f "${secretsFile}" ]]; then
  echo "secretsFile ${secretsFile} not available"
  exit 1
fi
declare tmpFile="$1.tmp"

jq --arg CLIENT_ID "${clientId}" --arg CLIENT_SECRET "${clientSecret}" \
  '.installed.client_id = $CLIENT_ID | .installed.client_secret = $CLIENT_SECRET' \
  "${secretsFile}" > "$tmpFile" && mv "$tmpFile" "${renderedSecretsFile}"
