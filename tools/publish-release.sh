#!/bin/bash

## This script builds a new GitHub release for the Terra CLI.
## Usage: ./publish-release.sh

echo "-- Building the source code archive"
sourceCodeArchiveFilename="terra-cli-${RELEASE_VERSION}.tar.gz"
sourceCodeArchivePath=../$sourceCodeArchiveFilename
tar -czf $sourceCodeArchivePath ./
echo "sourceCodeArchivePath: $sourceCodeArchivePath"
ls -la ..

# TODO: include Docker image build and publish, once Vault secrets have been added to the GH repo
#echo "-- Building the Docker image"
#./tools/build-docker.sh terra-cli/local forRelease
#echo "-- Publishing the Docker image"
#./tools/publish-docker.sh terra-cli/local forRelease "terra-cli/v1$RELEASE_VERSION" stable

echo "-- Building the distribution archive"
./gradlew distTar
distributionArchivePath=$(ls build/distributions/*tar)
echo "distributionArchivePath: $distributionArchivePath"
#mariko

echo "-- Creating a new GitHub release with the install archive and download script"
#gh release create --draft "${GITHUB_REF#refs/tags/}" dist/*.tar tools/download-install.sh
gh release create $RELEASE_VERSION \
  --draft \
  --title "v${RELEASE_VERSION}" \
  ${distributionArchivePath} \
#  "${distributionArchivePath}#Install package" \
#  "tools/download-install.sh#Download & Install script" \
#  "${sourceCodeArchivePath}#Source code"
