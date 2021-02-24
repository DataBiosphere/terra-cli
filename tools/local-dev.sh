#!/bin/bash

# Run this script from the top-level directory "terra-cli/".
# source tools/local-dev.sh

echo "Pulling the default Docker image from GCR"
./gradlew pullDockerImage

echo "Building Java code"
./gradlew install

echo "Aliasing JAR file"
alias terra=$(pwd)/build/install/terra-cli/bin/terra-cli

echo "Setting the Docker image id to the default"
terra app set-image --default