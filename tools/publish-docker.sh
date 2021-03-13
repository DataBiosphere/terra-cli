#!/bin/bash

## This script build the Docker image that the CLI uses to run applications.
## Dependencies: docker, gcloud
## Inputs: remoteImageTag (arg, required) tag of the image in GCR
##         remoteImageName (arg, required) name of the image in GCR
##         localImageTag (arg, required) tag of the local image
##         localImageName (arg, optional) name of the local image, default is 'terra-cli/local'
## Usage: ./tools/publish-docker.sh terra-cli/v0.0 stable

## The script assumes that it is being run from the top-level directory "terra-cli/".

usage="Usage: tools/publish-docker.sh [localImageName] [localImageTag] [remoteImageName] [remoteImageTag]"

# check required arguments
remoteImageTag=$1
remoteImageName=$2
localImageTag=$3
if [ -z "$remoteImageTag" ] || [ -z "$remoteImageName" ] || [ -z "$localImageTag" ]
  then
    echo $usage
    exit 1
fi

# set the local image name if no name was provided
localImageName=$4
if [ -z "$localImageName" ]
  then
    localImageName="terra-cli/local"
fi

echo "Logging in to docker using the CI service account key file"
cat rendered/ci-account.json | docker login -u _json_key --password-stdin https://gcr.io

echo "Tagging the local docker image with the name to use in GCR"
dockerGcrProject="terra-cli-dev"
localImageNameAndTag="$localImageName:$localImageTag"
remoteImageNameAndTag="gcr.io/$dockerGcrProject/$remoteImageName:$remoteImageTag"
docker tag $localImageNameAndTag $remoteImageNameAndTag

echo "Logging into to gcloud and configuring docker with the CI service account"
# reference: https://cloud.google.com/container-registry/docs/advanced-authentication#gcloud-helper
currentGcloudUser=$(gcloud config get-value account)
gcloud auth activate-service-account --key-file=rendered/ci-account.json
gcloud auth configure-docker

echo "Pushing the image to GCR"
docker push $remoteImageNameAndTag

echo "Restoring the current gcloud user"
gcloud config set account $currentGcloudUser

# write out the path to the remote image
echo "$remoteImageNameAndTag successfully pushed to GCR"
