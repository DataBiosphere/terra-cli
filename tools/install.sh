#!/bin/bash
set -e
## This script installs the Terra CLI from an unarchived release directory.
## TERRA_CLI_DOCKER_MODE environment variable controls docker support. Set to
##     DOCKER_NOT_AVAILABLE (default) to skip pulling the Docker image
#      or DOCKER_AVAILABLE to pull the image (requires Docker to be installed and running).
# # Dependencies: docker, gcloud
## Usage: ./install.sh

if [ -z "$TERRA_CLI_DOCKER_MODE" ] || [ "$TERRA_CLI_DOCKER_MODE" == "DOCKER_NOT_AVAILABLE" ]; then
  terraCliDockerMode="DOCKER_NOT_AVAILABLE"
elif [ "$TERRA_CLI_DOCKER_MODE" == "DOCKER_AVAILABLE" ]; then
  terraCliDockerMode="DOCKER_AVAILABLE"
else
  echo "Unsupported TERRA_CLI_DOCKER_MODE specified: $TERRA_CLI_DOCKER_MODE"
  exit 1
fi

echo "-- Docker availability mode is $terraCliDockerMode"

echo "--  Checking that script is being run from the same directory"
archiveDir=$PWD
if [ ! -f "$archiveDir/install.sh" ]; then
  echo "Script must be run from the same directory (i.e. ./install.sh, not terra-0.1.0/install.sh)"
  exit 1
fi

echo "--  Checking for application directory"
applicationDir="${HOME}/.terra"
mkdir -p "$applicationDir"
if [ ! -d "${applicationDir}" ]; then
    echo "Error creating application directory: ${applicationDir}"
    exit 1
fi

echo "--  Moving JARs to application directory"
if [ -f "${applicationDir}/lib" ] || [ -d "${applicationDir}/lib" ]; then
  rm -R "${applicationDir:?}/lib"
fi
cp -R "$archiveDir"/lib "$applicationDir"/lib

echo "--  Moving run script and README out of archive directory"
cp "$archiveDir"/bin/terra "$archiveDir"/../terra
cp "$archiveDir"/README.md "$archiveDir"/../README.md

echo "--  Deleting the archive directory"
cd "$archiveDir"/..
rm -R "$archiveDir"

if [ "$terraCliDockerMode" == "DOCKER_NOT_AVAILABLE" ]; then
  echo "Installing without docker image because TERRA_CLI_DOCKER_MODE is DOCKER_NOT_AVAILABLE."
  ./terra config set app-launch LOCAL_PROCESS
else
  echo "Setting the Docker image id to the default"
  ./terra config set image --default
  ./terra config set app-launch DOCKER_CONTAINER
  echo "Pulling the default Docker image"
  defaultDockerImage=$(./terra config get image)
  docker pull "$defaultDockerImage"
fi

echo "-- Setting the server to its current value, to pull any changes"
currentServer=$(./terra config get server)
./terra config set server --name="$currentServer"

echo "--  Install complete"
echo "You can add the ./terra executable to your \$PATH"
echo "Run \"./terra\" to see usage"
