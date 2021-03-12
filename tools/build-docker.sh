#!/bin/bash

## This script build the Docker image that the CLI uses to run applications.
## Dependencies: docker, git
## Inputs: localImageName (arg, required) name of the image on the local host
##         localImageTag (arg, optional) tag of the image on the local host, default is Git commit hash
## Usage: ./tools/build-docker.sh terra-cli/local
##        ./tools/build-docker.sh terra-cli/local test123

## The script assumes that it is being run from the top-level directory "terra-cli/".

usage="Usage: tools/publish-docker.sh [localImageName] [localImageTag]"

# check required arguments
localImageName=$1
if [ -z "$localImageName" ]
  then
    echo $usage
    exit 1
fi

# generate a tag from the commit hash if no tag was provided
localImageTag=$2
if [ -z "$localImageTag" ]; then
  echo "Generating an image tag from the Git commit hash"
  localImageTag=$(git rev-parse --short HEAD)
fi

echo "Building the image"
localImageNameAndTag="$localImageName:$localImageTag"
docker build -t $localImageNameAndTag ./docker

# write out the path to the local image
echo "$localImageNameAndTag successfully built"
