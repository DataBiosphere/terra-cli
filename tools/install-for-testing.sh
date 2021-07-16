#!/bin/bash

set -e
## This script installs the CLI for testing. It has two modes:
##  - SOURCE_CODE means that the CLI has already been installed directly from source code (./gradlew install).
#     this script just configures it and pulls the default docker image
##  - GITHUB_RELEASE means to download the latest CLI release from GitHub and install it using the bundled scripts
## Dependencies: docker, curl
## Usage: ./tools/install-for-testing.sh SOURCE_CODE
##        ./tools/install-for-testing.sh GITHUB_RELEASE

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [ $(basename $PWD) != 'terra-cli' ]; then
  echo "Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

installMode=$1
echo "installMode: $installMode"
if [ "$installMode" = "SOURCE_CODE" ]; then
  echo "Assuming the Java code is already built and installed by Gradle"
  terra=$(pwd)/build/install/terra-cli/bin/terra

  echo "Setting the Docker image id to the default"
  $terra config set image --default

  echo "Pulling the default Docker image"
  docker pull $($terra config get image)

elif [ "$installMode" = "GITHUB_RELEASE" ]; then
  echo "Creating a new build/test-install directory"
  mkdir -p $(pwd)/build/test-install/
  cd build/test-install/

  echo "Downloading the install script from GitHub and running it"
  curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash

else
  echo "Usage: tools/install-for-testing.sh [installMode]"
  echo "       installMode = SOURCE_CODE, GITHUB_RELEASE"
  exit 1
fi

