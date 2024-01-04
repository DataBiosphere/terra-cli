#!/bin/bash

## This script sets up the environment for local development.
## Dependencies: docker, chmod
## Usage: source tools/local-dev.sh

function check_java_version() {
  local REQ_JAVA_VERSION=17

  echo "--  Checking if installed Java version is ${REQ_JAVA_VERSION} or higher"
  if [[ -n "$(which java)" ]]; then
    # Get the current major version of Java: "11.0.12" => "11"
    local CUR_JAVA_VERSION="$(java -version 2>&1 | awk -F\" '{ split($2,a,"."); print a[1]}')"
    if [[ "${CUR_JAVA_VERSION}" -lt ${REQ_JAVA_VERSION} ]]; then
      >&2 echo "ERROR: Java version detected (${CUR_JAVA_VERSION}) is less than required (${REQ_JAVA_VERSION})"
      return 1
    fi
  else
    >&2 echo "ERROR: No Java installation detected"
    return 1
  fi
}

if ! check_java_version; then
  unset check_java_version
  return 1
fi
unset check_java_version

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [[ "$(basename "$PWD")" != 'terra-cli' ]]; then
  >&2 echo "ERROR: Script must be run from top-level directory 'terra-cli/'"
  return 1
fi

echo "Building Java code"
./gradlew clean install

echo "Aliasing JAR file"
alias terra="$(pwd)"/build/install/terra-cli/bin/terra

echo "Setting the Docker image id to the default"
terra config set image --default
terra config set app-launch DOCKER_CONTAINER
echo "Pulling the default Docker image"
defaultDockerImage=$(terra config get image)
docker pull sha256:fb149df709a05cf9c9fb22ccdb274b0e964cd07d4d61de194032311784bb4b5d

echo "Setting the server to its current value, to pull any changes"
currentServer=$(terra config get server)
terra config set server --name="$currentServer"

echo "Making all 'tools' scripts executable"
chmod a+x tools/*

# pull credentials needed for development
./tools/render-config.sh
