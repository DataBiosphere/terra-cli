#!/bin/bash

echo "Setting the gcloud project to the workspace project"
gcloud config set project ${GOOGLE_CLOUD_PROJECT}

echo