#!/bin/bash

## This script builds a new GitHub release for the Terra CLI, and uploads a new Docker container.
## The GitHub release includes an install package and a download + install script.
## Note that a pre-release does not affect the "Latest release" tag, but a regular release does.
## The release version number argument to this script must match the version number in the build.gradle
# file (i.e. version = '0.0.0' line).
## Dependencies: docker, gh, sed
## Inputs: releaseVersion (arg, required) determines the git tag to use for creating the release
##         isRegularRelease (arg, optional) 'false' for a pre-release (default), 'true' for a regular release
## Usage: ./publish-release.sh  0.0.0        --> publishes version 0.0.0 as a pre-release
## Usage: ./publish-release.sh  0.0.0 true   --> publishes version 0.0.0 as a regular release

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [ $(basename $PWD) != 'terra-cli' ]; then
  echo "Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

usage="Usage: tools/publish-release.sh [releaseVersion] [isRegularRelease]"

releaseVersion=$1
if [ -z "$releaseVersion" ]; then
    echo $usage
    exit 1
fi
isRegularRelease=$2
if [ "$isRegularRelease" != "true" ]; then
  isRegularRelease="false"
fi

echo "-- Validating version string"
# Docker image name cannot contain any uppercase letters, so this would prevent using the same
# version number for both the Java code and Docker image
if [[ $releaseVersion =~ [A-Z] ]]; then
  echo "Release version cannot contain any uppercase letters"
  exit 1
fi

echo "-- Checking if this version matches the value in build.gradle"
# note that the --quiet flag has to be before the task name, otherwise log statements
# related to downloading Gradle are not suppressed (https://github.com/gradle/gradle/issues/5098)
buildGradleVersion=$(./gradlew --quiet getBuildVersion)
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
./gradlew clean distTar -PforRelease
distributionArchivePath=$(ls build/distributions/*tar)
# don't include the version number in the archive file name, so that the install script doesn't need to know it
mv $distributionArchivePath build/distributions/terra-cli.tar
distributionArchivePath=$(ls build/distributions/*tar)

echo "-- Creating a new GitHub release with the install archive and download script"
gh config set prompt disabled
if [ "$isRegularRelease" == "true" ]; then
  echo "Creating regular release"
  preReleaseFlag=""
else
  echo "Creating pre-release"
  preReleaseFlag="--prerelease"
fi
gh release create $releaseTag $preReleaseFlag \
  --title "$releaseVersion" \
  "${distributionArchivePath}#Install package" \
  "tools/download-install.sh#Download & Install script"
