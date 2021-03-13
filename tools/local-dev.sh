#!/bin/bash

## This script sets up the machine for local development.
## Dependencies: docker, chmod
## Usage: source tools/local-dev.sh

## The script assumes that it is being run from the top-level directory "terra-cli/".

echo "Building Java code"
./gradlew clean install

echo "Aliasing JAR file"
alias terra=$(pwd)/build/install/terra-cli/bin/terra

echo "Setting the Docker image id to the default"
terra app set-image --default

echo "Pulling the default Docker image from GCR"
defaultDockerImage=$(terra app get-image)
docker pull $defaultDockerImage

echo "Making all 'tools' scripts executable"
chmod a+x tools/*

# pull credentials needed for development
./tools/render-config.sh