#!/bin/bash

set -e
## This script sets up the environment for local development.
## Dependencies: docker, chmod
## Usage: source tools/local-dev.sh

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [ $(basename $PWD) != 'terra-cli' ]; then
  echo "Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

echo "Building Java code"
./gradlew clean install

echo "Aliasing JAR file"
alias terra=$(pwd)/build/install/terra-cli/bin/terra

terra config list
terra config get image

echo "Setting the Docker image id to the default"
terra config set image --default

echo "Pulling the default Docker image"
defaultDockerImage=$(terra config get image)
docker pull $defaultDockerImage

echo "Setting the server to its current value, to pull any changes"
currentServer=$(terra config get server)
terra config set server --name=$currentServer

echo "Making all 'tools' scripts executable"
chmod a+x tools/*

# pull credentials needed for development
./tools/render-config.sh

# restore the bash setting to exit on a failing command
# (this script is typically called with `source`, and so we don't want to modify the calling shell)
set +e
