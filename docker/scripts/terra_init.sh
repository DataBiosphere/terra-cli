#!/bin/bash

echo "Setting up Terra app environment..."

gcloud auth activate-service-account --key-file=${PET_KEY_FILE}
gcloud config set project ${GOOGLE_PROJECT_ID}

echo "Done setting up Terra app environment..."
echo