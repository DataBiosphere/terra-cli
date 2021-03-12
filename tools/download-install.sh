#!/bin/bash

## This script downloads a release archive of the Terra CLI and calls its install script.
## Dependencies: curl, tar
## Inputs: TERRA_CLI_VERSION (env var, optional) specific version requested, default is latest
## Usage: ./download-install.sh        --> installs the latest version
##        ./download-install.sh 0.1    --> installs version 0.1

echo "--  Checking if specific version requested"
if [[ -z "${TERRA_CLI_VERSION}" ]]; then
  terraCliVersion="latest"
else
  terraCliVersion=$TERRA_CLI_VERSION
fi
releaseName="terra-${terraCliVersion}"

echo "--  Downloading release archive from GitHub"
# TODO: curl github releases page here
archiveFilename="${releaseName}.tar"
archiveFilePath="/Users/marikomedlock/Workspaces/terra-cli/build/distributions/${archiveFilename}"
if [ ! -f "${archiveFilePath}" ]; then
    echo "Error downloading release: ${archiveFilename}"
    exit 1
fi

echo "--  Unarchiving release"
scratchInstallDir=$PWD
mkdir -p $scratchInstallDir
tar -C $scratchInstallDir -xvf $archiveFilePath
if [ ! -d "${scratchInstallDir}/${releaseName}" ]; then
    echo "Error unarchiving release: ${archiveFilePath}"
    exit 1
fi

echo "--  Running the install script inside the release directory"
currentDir=$PWD
cd $scratchInstallDir/$releaseName
./install.sh
cd $currentDir
