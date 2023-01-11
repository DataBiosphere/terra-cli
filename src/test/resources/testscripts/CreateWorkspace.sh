#!/bin/bash
set -e
## This script checks if there is a logged in user. If there is, it creates a new workspace.

terra status
terra auth status

isLoggedIn=$(terra auth status --format=json | jq .loggedIn)
if [[ "true" == "$isLoggedIn" ]]; then
  currentUser=$(terra auth status --format=json | jq .userEmail)
  echo "User $currentUser is logged in. Creating a new workspace."
  terra workspace create --id=my-workspace-$RANDOM
  terra auth status
  # Polling for GCP permissions is difficult as the test user may not have gcloud credentials available,
  # so instead this is a static wait to compensate for the delay in syncing IAM permissions in GCP.
  sleep 120
else
  echo "No user is logged in. Skipping creating a new workspace."
fi
