#!/bin/bash

echo "Setting up Terra app environment..."

gcloud auth activate-service-account --key-file=${GOOGLE_APPLICATION_CREDENTIALS}
gcloud config set project ${GOOGLE_CLOUD_PROJECT}

echo "Done setting up Terra app environment..."
echo