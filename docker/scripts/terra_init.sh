#!/bin/bash

echo "Setting up Terra app environment..."

gcloud auth activate-service-account --key-file=${TERRA_PET_KEY_FILE}
gcloud config set project ${TERRA_GOOGLE_PROJECT_ID}

echo "Done setting up Terra app environment..."
echo