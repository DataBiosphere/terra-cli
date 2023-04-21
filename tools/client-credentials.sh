#!/bin/bash
set -e
## This script package client id and secrets into relevant files
## The GitHub release includes an install package and a download + install script.
## Note that a pre-release does not affect the "Latest release" tag, but a regular release does.
## The release version number argument to this script must match the version number in the settings.gradle
# file (i.e. version = '0.0.0' line).
#
## Dependencies: jq
## Inputs: secretsFile (arg, required) path to the secrets file (valid format)
##         clientId (arg, required) client id
##         clientSecret (arg, required) client secret
## Usage: ./client-credentials.sh <file> <id> <secret> --> adds client_id and client_secret to file

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [[ $(basename "$PWD") != 'terra-cli' ]]; then
  >&2 echo "ERROR: Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

secretsFile=$1
clientId=$2
clientSecret=$3

if [ ! -f "$secretsFile" ]; then
  echo "$secretsFile not available, skipping"
  exit 0
fi
tmpFile=$1+".tmp"

if [ -z "$clientId" ] || [ -z "$clientSecret" ]; then
  echo "client_id & client_secret not available for $secretsFile, skipping"
  exit 0
fi

jq --arg CLIENT_ID "$clientId" --arg CLIENT_SECRET "$clientSecret" \
  '.installed.client_id = $CLIENT_ID | .installed.client_secret = $CLIENT_SECRET' \
  "$secretsFile" > "$tmpFile" && mv "$tmpFile" "$secretsFile"
