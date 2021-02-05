#!/bin/bash

# Run this script from the top-level directory "terra-cli/".
# source tools/local-dev.sh

# build Docker image
docker build -t terra/cli:v0.0 ./docker

# build and alias JAR file
./gradlew install
alias terra=$(pwd)/build/install/terra-cli/bin/terra-cli
