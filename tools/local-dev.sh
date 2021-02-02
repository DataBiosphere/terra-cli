#!/bin/bash

# Run this script from the top-level directory "terra-cli/".
# sh tools/local-dev.sh

# build Docker image
docker build -t terra/cli:v0.0 ./docker

# build JAR file
./gradlew install
