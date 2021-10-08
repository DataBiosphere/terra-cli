#!/bin/bash

echo "Setting the gcloud project to the workspace project"
gcloud config set project ${GOOGLE_CLOUD_PROJECT}

if [ -n "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
  echo "Setting the gcloud credentials to the user's pet service account"
  gcloud auth activate-service-account --key-file=${GOOGLE_APPLICATION_CREDENTIALS}
fi

printenv

echo