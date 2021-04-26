#!/bin/bash

echo "Setting up Terra app environment..."

gcloud auth activate-service-account --key-file=${GOOGLE_APPLICATION_CREDENTIALS}
gcloud config set project ${GOOGLE_CLOUD_PROJECT}

# DO NOT SUBMIT revert me.
touch hello.txt

echo "Done setting up Terra app environment..."
echo