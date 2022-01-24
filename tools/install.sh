#!/bin/bash
set -e
## This script installs the Terra CLI from an unarchived release directory.
## Dependencies: docker, gcloud
## Usage: ./install.sh

echo "--  Determining installation mode"
if [[ -z "$TERRA_CLI_INSTALLATION_MODE" ]]; then
  terraCliInstallationMode="SUPPORT_DOCKER"
else
  terraCliInstallationMode="LOCAL_ONLY"
fi

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

echo "--  Setting the Docker image id to the default"
./terra config set image --default

if [ "$terraCliInstallationMode" == "SUPPORT_DOCKER" ]; then
  echo "--  Pulling the default Docker image"
  defaultDockerImage=$(./terra config get image)
  docker pull "$defaultDockerImage"
else
  echo "-- No Docker support required. Will not pull image."
fi

echo "-- Setting the server to its current value, to pull any changes"
currentServer=$(./terra config get server)
./terra config set server --name="$currentServer"

echo "--  Install complete"
echo "You can add the ./terra executable to your \$PATH"
echo "Run \"./terra\" to see usage"
