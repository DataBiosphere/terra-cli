#!/bin/bash
set -e
## This script builds a new GitHub release for the Terra CLI, and uploads a new Docker container.
## The GitHub release includes an install package and a download + install script.
## Note that a pre-release does not affect the "Latest release" tag, but a regular release does.
## The release version number argument to this script must match the version number in the settings.gradle
# file (i.e. version = '0.0.0' line).
#
## Dependencies: docker, gh, sed, jq
## Inputs: releaseVersion (arg, required) determines the git tag to use for creating the release
##         isRegularRelease (arg, optional) 'false' for a pre-release (default), 'true' for a regular release
## Usage: ./publish-release.sh  0.0.0        --> publishes version 0.0.0 as a pre-release
## Usage: ./publish-release.sh  0.0.0 true   --> publishes version 0.0.0 as a regular release

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [[ $(basename "$PWD") != 'terra-cli' ]]; then
  >&2 echo "ERROR: Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

releaseVersion=$1
if [[ -z "$releaseVersion" ]]; then
    >&2 echo "ERROR: Usage: tools/publish-release.sh [releaseVersion] [isRegularRelease]"
    exit 1
fi
isRegularRelease=$2
if [[ "$isRegularRelease" != "true" ]]; then
  isRegularRelease="false"
fi

echo "-- Validating version string"
# Docker image name cannot contain any uppercase letters, so this would prevent using the same
# version number for both the Java code and Docker image
if [[ $releaseVersion =~ [A-Z] ]]; then
  >&2 echo "ERROR: Release version cannot contain any uppercase letters"
  exit 1
fi

echo "-- Checking if this version matches the value in settings.gradle"
# note that the --quiet flag has to be before the task name, otherwise log statements
# related to downloading Gradle are not suppressed (https://github.com/gradle/gradle/issues/5098)
buildGradleVersion=$(./gradlew --quiet getBuildVersion)
if [[ "$releaseVersion" != "$buildGradleVersion" ]]; then
  >&2 echo "ERROR: Release version ($releaseVersion) does not match settings.gradle version ($buildGradleVersion)"
  exit 1
else
  echo "Release version matches settings.gradle version"
fi

echo "-- Checking if there is a tag that matches this version"
releaseTag=$releaseVersion
foundReleaseTag=$(git tag -l "$releaseVersion")
if [[ -z "$foundReleaseTag" ]]; then
  >&2 echo "ERROR: No tag found matching this version"
  exit 1
else
  echo "Found tag matching this version"
fi

echo "-- Building the Docker image"
./tools/build-docker.sh forRelease

echo "-- Publishing the Docker image"
dockerImageName=$(./gradlew --quiet getDockerImageName) # e.g. terra-cli
dockerImageTag=$(./gradlew --quiet getDockerImageTag) # e.g. stable
./tools/publish-docker.sh "$dockerImageTag" "$dockerImageName/$releaseVersion" forRelease

echo "-- Building the distribution archive"
./gradlew clean distTar -PforRelease
distributionArchivePath=$(ls build/distributions/*tar)

# Function to package client id and secrets into relevant files
# params: $1: secretsFile
#         $2: client_id
#         $3: client_secret
function packageAppSecrets() {
  if [ -f "$1" ]; then
    echo "$1 not available, skipping"
    return
  fi
  tmpFile=$1+".tmp"

  if [ -z "$2" ] || [ -z "$3" ]; then
    echo "client_id & client_secret not available for $1, skipping"
    return
  fi

  jq --arg CLIENT_ID "$2" --arg CLIENT_SECRET "$3" \
    '.installed.client_id = $CLIENT_ID | .installed.client_secret = $CLIENT_SECRET' \
    "$1" > "$tmpFile" && mv "$tmpFile" "$1"
}

echo "-- Packaging client id and client secrets in the release"
packageAppSecrets "src/main/resources/broad_secret.json" "$BROAD_CLIENT_ID" "$BROAD_CLIENT_SECRET"
packageAppSecrets "src/main/resources/verily_secret.json" "$VERILY_CLIENT_ID" "$VERILY_CLIENT_SECRET"

echo "-- Creating a new GitHub release with the install archive and download script"
gh config set prompt disabled
if [[ "$isRegularRelease" == "true" ]]; then
  echo "Creating regular release"
  preReleaseFlag=""
else
  echo "Creating pre-release"
  preReleaseFlag="--prerelease"
fi
gh release create "$releaseTag" $preReleaseFlag \
  --title "$releaseVersion" \
  "${distributionArchivePath}#Install package" \
  "tools/download-install.sh#Download & Install script"
