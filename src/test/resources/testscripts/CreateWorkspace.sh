#!/bin/bash
set -e
## This script checks if there is a logged in user. If there is, it creates a new workspace.

terra status
terra auth status

isLoggedIn=$(terra auth status --format=json | jq .loggedIn)
if [[ "true" = "$isLoggedIn" ]]
then
  currentUser=$(terra auth status --format=json | jq .userEmail)
  echo "User $currentUser is logged in. Creating a new workspace."
  terra workspace create
  terra auth status
else
  echo "No user is logged in. Skipping creating a new workspace."
fi

cat /Users/marikomedlock/Workspaces/terra-cli/build/test-context/.terra/context.json