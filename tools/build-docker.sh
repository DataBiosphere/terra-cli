#!/bin/bash

# Run this script from the top-level directory "terra-cli/".
# e.g. tools/build-docker.sh terra-cli/local

usage="Usage: tools/publish-docker.sh [localImageName]"

# check required arguments
localImageName=$1
if [ -z "$localImageName" ]
  then
    echo $usage
    exit 1
fi

echo "Generating an image tag from the Git commit hash"
gitHash=$(git rev-parse --short HEAD)

echo "Building the image"
localImageNameAndTag="$localImageName:$gitHash"
docker build -t $localImageNameAndTag ./docker

# write out the path to the local image
echo "$localImageNameAndTag successfully built"
