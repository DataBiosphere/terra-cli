#!/bin/bash

## This script builds a new GitHub release for the Terra CLI, and uploads a new Docker container.
## The GitHub release includes an install package and a download + install script.
## Note that a pre-release does not affect the "Latest release" tag, but a regular release does.
## Dependencies: docker, gh, sed
## Inputs: releaseVersion (arg, required) determines the git tag to use for creating the release
##         isPreRelease (arg, optional) 'true' for a pre-release (default), 'false' for a regular release
## Usage: ./publish-release.sh  0.0        --> publishes version 0.0 as a pre-release
## Usage: ./publish-release.sh  0.0 false  --> publishes version 0.0 as a regular release

usage="Usage: tools/publish-release.sh [releaseVersion]"

releaseVersion=$1
if [ -z "$releaseVersion" ]; then
    echo $usage
    exit 1
fi
isPreRelease=$2
if [ "$isPreRelease" != "false" ]; then
  isPreRelease="true"
fi

echo "-- Checking if this version contains any uppercase letters"
# Docker image name cannot contain any uppercase letters, so this would prevent using the same
# version number for both the Java code and Docker image
if [[ $releaseVersion =~ [A-Z] ]]; then
  echo "Release version cannot contain any uppercase letters"
  exit 1
fi

echo "-- Checking if this version matches the value in build.gradle"
buildGradleVersion=$(sed -n -e "/^version/ s/.*\= *\'\(.*\)\'/\1/p" build.gradle)
if [ "$releaseVersion" != "$buildGradleVersion" ]; then
  echo "Release version ($releaseVersion) does not match build.gradle version ($buildGradleVersion)"
  exit 1
else
  echo "Release version matches build.gradle version"
fi

echo "-- Checking if there is a tag that matches this version"
releaseTag=$releaseVersion
foundReleaseTag=$(git tag -l $releaseVersion)
if [ -z "$foundReleaseTag" ]; then
  echo "No tag found matching this version"
  exit 1
else
  echo "Found tag matching this version"
fi

echo "-- Building the Docker image"
./tools/build-docker.sh forRelease

echo "-- Publishing the Docker image"
./tools/publish-docker.sh stable "terra-cli/$releaseVersion" forRelease

echo "-- Building the distribution archive"
./gradlew clean distTar
distributionArchivePath=$(ls build/distributions/*tar)
mv $distributionArchivePath build/distributions/terra-cli.tar
distributionArchivePath=$(ls build/distributions/*tar)

echo "-- Creating a new GitHub release with the install archive and download script"
gh config set prompt disabled
if [ "$isPreRelease" == "true" ]; then
  preReleaseFlag="--prerelease"
else
  preReleaseFlag=""
fi
gh release create $releaseTag $preReleaseFlag \
  --title "$releaseVersion" \
  "${distributionArchivePath}#Install package" \
  "tools/download-install.sh#Download & Install script"
