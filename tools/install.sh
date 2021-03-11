#!/bin/bash

## This script installs the Terra CLI from an unarchived release directory.
## Dependencies: docker
## Usage: ./install.sh

## The script assumes that it is being run from the same directory (i.e. ./install.sh, not terra-0.1/install.sh)
ARCHIVE_DIR=$PWD

echo "Checking for application directory"
APPLICATION_DIRECTORY="${HOME}/.terra"
mkdir -p $APPLICATION_DIRECTORY
if [ ! -d "${APPLICATION_DIRECTORY}" ]; then
    echo "Error creating application directory: ${APPLICATION_DIRECTORY}"
    exit 1
fi

echo "Moving JARs to application directory"
if [ -f "${APPLICATION_DIRECTORY}/lib" ]; then
  rm -R "${APPLICATION_DIRECTORY}/lib"
fi
mv "${ARCHIVE_DIR}/lib" $APPLICATION_DIRECTORY

echo "Moving run script out of archive directory"
mv "${ARCHIVE_DIR}/bin/terra" "${ARCHIVE_DIR}/../terra"

echo "Deleting the archive directory"
cd "${ARCHIVE_DIR}/.."
rm -R $ARCHIVE_DIR

echo "Pulling the default Docker image"
./terra app set-image --default
DEFAULT_DOCKER_IMAGE=$(./terra app get-image)
echo "default docker image: ${DEFAULT_DOCKER_IMAGE}"
docker pull $DEFAULT_DOCKER_IMAGE

echo "Install complete"
echo "You can add the ./terra executable to your \$PATH"
echo "Run \"terra --help\" to see usage"