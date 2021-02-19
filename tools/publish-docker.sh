#!/bin/bash

# Run this script from the top-level directory "terra-cli/".
# e.g. tools/publish-docker.sh terra-cli/local test123 terra-cli/v0.0 stable

usage="Usage: tools/publish-docker.sh [localImageName] [localImageTag] [remoteImageName] [remoteImageTag]"

# check required arguments
localImageName=$1
localImageTag=$2
remoteImageName=$3
remoteImageTag=$4
if [ -z "$localImageName" ] || [ -z "$localImageTag" ] || [ -z "$remoteImageName" ] || [ -z "$remoteImageTag" ]
  then
    echo $usage
    exit 1
fi

echo "Reading the CI service account key file from Vault"
vault read -format json secret/dsde/terra/kernel/dev/common/ci/ci-account.json | jq .data > rendered/ci-account.json

echo "Logging in to docker using this key file"
cat rendered/ci-account.json | docker login -u _json_key --password-stdin https://gcr.io

echo "Tagging the local docker image with the name to use in GCR"
localImageNameAndTag="$localImageName:$localImageTag"
remoteImageNameAndTag="$remoteImageName:$remoteImageTag"
docker tag $localImageNameAndTag $remoteImageNameAndTag

echo "Pushing the image to GCR"
docker push $remoteImageNameAndTag

# write out the path to the remote image
echo "$remoteImageNameAndTag successfully pushed to GCR"
