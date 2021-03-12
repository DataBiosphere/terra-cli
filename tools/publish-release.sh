#!/bin/bash

## This script builds a new GitHub release for the Terra CLI.
## Usage: ./publish-release.sh        --> installs the latest version

echo "-- Building the source code archive"
SOURCE_CODE_ARCHIVE_FILENAME="terra-cli-$RELEASE_VERSION.tar.gz"
SOURCE_CODE_ARCHIVE_PATH=../$SOURCE_CODE_ARCHIVE_FILENAME
tar -czf $SOURCE_CODE_ARCHIVE_PATH ./

# TODO: build and publish the Docker image

echo "-- Building the distribution archive"
./gradlew distTar
ls -la build/distributions
ls -la tools/

echo "-- Creating a new GitHub release with the install archive and download script"
#gh release create --draft "${GITHUB_REF#refs/tags/}" dist/*.tar tools/download-install.sh
echo "tags: ${GITHUB_REF#refs/tags/}"
gh release create "${GITHUB_REF#refs/tags/}" \
  --draft \
  --title "$RELEASE_VERSION" \
  'build/distributions/*.tar#Install package' \
  'tools/download-install.sh#Download & Install script' \
  '$SOURCE_CODE_ARCHIVE_PATH#Source code'
