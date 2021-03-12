#!/bin/bash

## This script sets up the machine for local development.
## Usage: source tools/local-dev.sh

## The script assumes that it is being run from the top-level directory "terra-cli/".

echo "Pulling the default Docker image from GCR"
./gradlew pullDockerImage

echo "Building Java code"
./gradlew install

echo "Aliasing JAR file"
alias terra=$(pwd)/build/install/terra-cli/bin/terra

echo "Setting the Docker image id to the default"
terra app set-image --default

# pull credentials needed for development
./tools/render-config.sh