#!/bin/bash
set -e
## This script configures the CLI before running an integration test.

terra status

# setup logging for testing (console = OFF, file = DEBUG)
terra config set logging --console --level=OFF
terra config set logging --file --level=DEBUG

# set the server to the one specified by the test
# (see the Gradle unitTest task for how this system property gets set from a Gradle property)
terra server set --name=$TERRA_SERVER

# set the docker image id to the one specified by the test, or to the default if it's unspecified
if [[ -z "${TERRA_DOCKER_IMAGE}" ]]; then
  terra config set image --default
else
  terra config set image --image="$TERRA_DOCKER_IMAGE"
fi

terra config list
