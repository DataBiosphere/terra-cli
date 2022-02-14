#!/usr/bin/env bash

# Usage: ./tools/native_image_agent <terra-arguments>

# Run the Native Image Agent with the GraalVM JVM to obtain necessary configuration options.
# See https://www.graalvm.org/22.0/reference-manual/native-image/Agent/
# The salient part is -agentlib:native-image-agent=config-output-dir=./build/config_out
# Prerequisite: GraalVM
# The application should be built, and we need something in ~/.terra (e.g. from local_dev.sh).
# This script is only semi-automatic: changes to the application will require updating
# the CLASSPATH below.

# Requirements: GraalVM, jenv (recommended)

JAVA_VERSION=$(java -version 2>&1)

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [ "$(basename "$PWD")" != 'terra-cli' ]; then
  echo "Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

if [[ "$JAVA_VERSION" != *"GraalVM"* ]]; then
  echo "GraalVM should be the active JVM."
  exit 1
fi

# Any terra arguments should be passed to this script. Not all terra sessions will use all reflected
# classes. For example, in order to instrument the Google OAuth code, do something like `workspace list`
# or `auth login`. It's expected that the config files we need will ultimately be the union of
# generated configs from several terra invocations.

# Directory for merging generated configs across many runs with different inputs.
# See https://docs.oracle.com/en/graalvm/enterprise/22/docs/reference-manual/native-image/Agent/
# The expectation is that this directory is in source control.
MERGE_DIR=./src/main/resources/META-INF/native-image

# Set jenv to GraalVM (change as necessary). Alternatively, set $JAVA_HOME and possibly other things.
# jenv shell graalvm64-11.0.14

# Run build classes in GraalVM with the native agent collecting config info.
java \
  -agentlib:native-image-agent=config-merge-dir=$MERGE_DIR \
  --add-opens java.base/sun.net.www.protocol.https=ALL-UNNAMED \
  --add-opens java.base/java.net=ALL-UNNAMED \
  -classpath "./build/install/terra-cli/lib/*:" \
  bio.terra.cli.command.Main "$@"

