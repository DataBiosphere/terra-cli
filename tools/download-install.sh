#!/bin/bash
set -e
## This script downloads a release archive of the Terra CLI and calls its install script.
## Dependencies: curl, tar
## Inputs: TERRA_CLI_VERSION (env var, optional) specific version requested, default is latest
## Usage: ./download-install.sh                            --> installs the latest version
##        TERRA_CLI_VERSION=0.1.0 ./download-install.sh    --> installs version 0.1.0

readonly REQ_JAVA_VERSION=17
echo "--  Checking if installed Java version is ${REQ_JAVA_VERSION} or higher"
if [[ -n "$(which java)" ]];
then
  # Get the current major version of Java: "11.0.12" => "11"
  readonly CUR_JAVA_VERSION="$(java -version 2>&1 | awk -F\" '{ split($2,a,"."); print a[1]}')"
  if [[ "${CUR_JAVA_VERSION}" -lt ${REQ_JAVA_VERSION} ]];
  then
    echo "Java version set to ${CUR_JAVA_VERSION}, is \$JAVA_HOME set correctly?, exiTing..."
    exit 1
  fi
else
  echo "Java installation not found, exiting"
  exit 1
fi

echo "--  Checking if specific version requested"
if [[ -z "${TERRA_CLI_VERSION}" ]]; then
  terraCliVersion="latest"
  echo "No specific version requested, using $terraCliVersion"
else
  terraCliVersion=$TERRA_CLI_VERSION
  echo "Specific version requested: $terraCliVersion"
fi

echo "--  Downloading release archive from GitHub"
ghRepoReleasesUrl="https://github.com/DataBiosphere/terra-cli/releases"
installPackageFileName="terra-cli.tar"
if [ "$terraCliVersion" == "latest" ]; then
  releaseTarUrl="$ghRepoReleasesUrl/latest/download/$installPackageFileName"
else
  releaseTarUrl="$ghRepoReleasesUrl/download/$terraCliVersion/$installPackageFileName"
fi

archiveFileName="terra-cli.tar"
curl -L "$releaseTarUrl" > $archiveFileName
if [ ! -f "${archiveFileName}" ]; then
    echo "Error downloading release: ${archiveFileName}"
    exit 1
fi

echo "--  Unarchiving release"
archiveDir=$PWD/terra-cli-install
rm -rf "$archiveDir"
mkdir -p "$archiveDir"
tar -C "$archiveDir" --strip-components=1 -xf $archiveFileName
if [ ! -f "$archiveDir/install.sh" ]; then
    echo "Error unarchiving release: ${archiveFileName}"
    exit 1
fi

echo "--  Running the install script inside the release directory"
currentDir=$PWD
cd "$archiveDir"
./install.sh
cd "$currentDir"

echo "-- Deleting the release archive"
rm $archiveFileName
