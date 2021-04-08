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

echo "Setting the Docker image id to the default"
terra config set image --default

echo "Pulling the default Docker image"
defaultDockerImage=$(terra config get-value image)
docker pull $defaultDockerImage

echo "Making all 'tools' scripts executable"
chmod a+x tools/*

# pull credentials needed for development
./tools/render-config.sh
