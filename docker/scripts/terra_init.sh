#!/bin/bash

if [ -n "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
  echo "Setting the gcloud credentials to match the application default credentials"
  gcloud auth activate-service-account --key-file=${GOOGLE_APPLICATION_CREDENTIALS}
fi

echo "Setting the gcloud project to the workspace project"
gcloud config set project ${GOOGLE_CLOUD_PROJECT}

echo