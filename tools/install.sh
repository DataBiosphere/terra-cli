#!/bin/bash

## This script installs the Terra CLI from an unarchived release directory.
## Dependencies: docker
## Usage: ./install.sh

## The script assumes that it is being run from the same directory (i.e. ./install.sh, not terra-0.1/install.sh)
archiveDir=$PWD

echo "--  Checking for application directory"
applicationDir="${HOME}/.terra"
mkdir -p $applicationDir
if [ ! -d "${applicationDir}" ]; then
    echo "Error creating application directory: ${applicationDir}"
    exit 1
fi

echo "--  Moving JARs to application directory"
if [ -f "${applicationDir}/lib" ] || [ -d "${applicationDir}/lib" ]; then
  rm -R "${applicationDir}/lib"
fi
cp -R $archiveDir/lib $applicationDir/lib

echo "--  Moving run script and README out of archive directory"
cp $archiveDir/bin/terra $archiveDir/../terra
cp $archiveDir/README.md $archiveDir/../README.md

echo "--  Deleting the archive directory"
cd $archiveDir/..
rm -R $archiveDir

echo "--  Setting the Docker image id to the default"
./terra app set-image --default

echo "--  Pulling the default Docker image"
defaultDockerImage=$(./terra app get-image)
docker pull $defaultDockerImage

echo "--  Install complete"
echo "You can add the ./terra executable to your \$PATH"
echo "Run \"./terra\" to see usage"