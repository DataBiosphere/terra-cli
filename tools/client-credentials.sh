#!/bin/bash
set -e
## This script adds client id and secrets into relevant files
#
## Dependencies: jq
## Inputs: secretsFile (arg, required) path to the secrets file (valid format)
##         renderedSecretsFile (arg, required) path to rendered secrets file.
##         clientId (arg, required) client id
##         clientSecret (arg, required) client secret
## Usage: ./client-credentials.sh <file> <renderedFile> <id> <secret> --> rendered client_id & client_secret to file saving it as renderedFile

secretsFile=$1
renderedSecretsFile=$2
clientId=$3
clientSecret=$4

if [ -z "$secretsFile" ] || [ -z "$renderedSecretsFile" ] || [ "$secretsFile" = "$renderedSecretsFile" ]; then
  echo "secretsFile & renderedSecretsFile paths are required and must be different"
  exit 1
fi

if [ ! -f "$secretsFile" ]; then
  echo "secrets file $secretsFile not available"
  exit 1
fi

if [ -z "$clientId" ] || [ -z "$clientSecret" ]; then
  echo "client_id & client_secret not available for $secretsFile"
  exit 1
fi

jq --arg CLIENT_ID "$clientId" --arg CLIENT_SECRET "$clientSecret" \
  '.installed.client_id = $CLIENT_ID | .installed.client_secret = $CLIENT_SECRET' \
  "$secretsFile" > "$renderedSecretsFile"
