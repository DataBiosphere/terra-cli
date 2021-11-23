#!/bin/bash
set -e
## This script checks if there is a logged in user. If there is, it deletes the current workspace.

terra status
terra auth status

isLoggedIn=$(terra auth status --format=json | jq .loggedIn)
if [[ "true" = "$isLoggedIn" ]]
then
  currentUser=$(terra auth status --format=json | jq .userEmail)
  echo "User $currentUser is logged in. Deleting the current workspace."
  terra workspace delete --quiet
else
  echo "No user is logged in. Skipping deleting the current workspace."
fi
