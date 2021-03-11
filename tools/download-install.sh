#!/bin/bash

## This script downloads a release archive of the Terra CLI and calls its install script.
## Dependencies: curl, tar
## Usage: ./download-install.sh        --> installs the latest version
##        ./download-install.sh 0.1    --> installs version 0.1

echo "--  Checking if specific version requested"
if [[ -z "${TERRA_CLI_VERSION}" ]]; then
  TERRA_CLI_VERSION="latest"
fi
RELEASE_NAME="terra-${TERRA_CLI_VERSION}"

echo "--  Downloading release archive from GitHub"
# TODO: curl github releases page here
ARCHIVE_FILENAME="${RELEASE_NAME}.tar"
ARCHIVE_FILE="/Users/marikomedlock/Workspaces/terra-cli/build/distributions/${ARCHIVE_FILENAME}"
if [ ! -f "${ARCHIVE_FILE}" ]; then
    echo "Error downloading release: ${ARCHIVE_FILENAME}"
    exit 1
fi

echo "--  Unarchiving release"
SCRATCH_INSTALL_DIR=$PWD
mkdir -p $SCRATCH_INSTALL_DIR
tar -C $SCRATCH_INSTALL_DIR -xvf $ARCHIVE_FILE
if [ ! -d "${SCRATCH_INSTALL_DIR}/${RELEASE_NAME}" ]; then
    echo "Error unarchiving release: ${ARCHIVE_FILE}"
    exit 1
fi

echo "--  Running the install script inside the release directory"
CURRENT_DIR=$PWD
cd $SCRATCH_INSTALL_DIR/$RELEASE_NAME
./install.sh
cd $CURRENT_DIR
