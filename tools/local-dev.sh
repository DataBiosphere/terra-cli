#!/bin/bash

set -e
## This script sets up the environment for local development.
## TERRA_CLI_DOCKER_MODE environment variable controls docker support. Set to
##     DOCKER_NOT_AVAILABLE (default) to skip pulling the Docker image
#      or DOCKER_AVAILABLE to pull the image (requires Docker to be installed and running).
# # Dependencies: docker, chmod
## Usage: source tools/local-dev.sh

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [ "$(basename "$PWD")" != 'terra-cli' ]; then
  echo "Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

if [ -z "$TERRA_CLI_DOCKER_MODE" ] || [ "$TERRA_CLI_DOCKER_MODE" == "DOCKER_NOT_AVAILABLE" ]; then
  terraCliDockerMode="DOCKER_NOT_AVAILABLE"
elif [ "$TERRA_CLI_DOCKER_MODE" == "DOCKER_AVAILABLE" ]; then
  terraCliDockerMode="DOCKER_AVAILABLE"
else
  echo "Unsupported TERRA_CLI_DOCKER_MODE specified: $TERRA_CLI_DOCKER_MODE"
  exit 1
fi

echo "Docker availability mode is $terraCliDockerMode"

echo "Building Java code"
./gradlew clean install

echo "Aliasing JAR file"
alias terra="$(pwd)"/build/install/terra-cli/bin/terra

if [ "$terraCliDockerMode" == "DOCKER_NOT_AVAILABLE" ]; then
  echo "Installing without docker image because TERRA_CLI_DOCKER_MODE is DOCKER_NOT_AVAILABLE."
  terra config set app-launch LOCAL_PROCESS
else
  echo "Setting the Docker image id to the default"
  terra config set image --default
  terra config set app-launch DOCKER_CONTAINER
  echo "Pulling the default Docker image"
  defaultDockerImage=$(terra config get image)
  docker pull "$defaultDockerImage"
fi

echo "Setting the server to its current value, to pull any changes"
currentServer=$(terra config get server)
terra config set server --name="$currentServer"

echo "Making all 'tools' scripts executable"
chmod a+x tools/*

# pull credentials needed for development
./tools/render-config.sh

# restore the bash setting to exit on a failing command
# (this script is typically called with `source`, and so we don't want to modify the calling shell)
set +e
