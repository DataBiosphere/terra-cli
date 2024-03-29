#!/bin/bash
set -e
## This script builds the Docker image that the CLI uses to run applications.
## Dependencies: docker, git
## Inputs: localImageTag (arg, optional) tag of the local image, default is Git commit hash
##         localImageName (arg, optional) name of the local image, default is 'terra-cli/local'
## Usage: ./tools/build-docker.sh                            --> builds local Docker image terra-cli/local:[GITHASH]
##        ./tools/build-docker.sh test123                    --> builds local Docker image terra-cli/local:test123
##        ./tools/build-docker.sh test123 terracli/branchA   --> builds local Docker image terracli/branchA:test123

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [[ $(basename $PWD) != 'terra-cli' ]]; then
  >&2 echo "ERROR: Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

# generate a tag from the commit hash if no tag was provided
localImageTag=$1
if [[ -z "$localImageTag" ]]; then
  echo "Generating an image tag from the Git commit hash"
  localImageTag=$(git rev-parse --short HEAD)
fi

# set the local image name if no name was provided
localImageName=$2
if [[ -z "$localImageName" ]]; then
    localImageName="terra-cli/local"
fi

# Images built on Apple M1 macs are for arm64 platform, force x86 build for docker
echo "Building the image"
localImageNameAndTag="$localImageName:$localImageTag"
targetPlatform="--platform=linux/amd64"
docker build -t $localImageNameAndTag $targetPlatform ./docker

# write out the path to the local image
echo "$localImageNameAndTag successfully built"
