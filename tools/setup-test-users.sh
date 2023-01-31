#!/bin/bash

set -e
## This script sets up users for running CLI tests. It only needs to be run once per SAM instance.
## Keep this script in sync with test users in your testconfig file (eg `testconfig/broad.json`).
##
## If the current server requires users to be invited before they can register, then the user who runs this
## script must be an admin user (i.e. a member of the fc-admins Google group in the SAM Gsuite). The script
## invites all the test users if they do not already exist in SAM, and this requires admin permissions.
##
## The admin users group email argument for this script should be the email address of a SAM group that contains
## several admin emails
## [developer-admins-devel|developer-admins-autopush|developer-admins-staging|developer-admins-preprod|developer-admins].
## This is to prevent the team from losing access if the person who originally ran this script is not available.
##
## Dependencies: jq
## Inputs: adminUsersGroupEmail (arg, required) email address of the SAM group for admin users
##         testConfigFile (arg, required) relative path to the test config file
## Usage: ./tools/setup-test-users.sh  developer-admins@dev.test.firecloud.org src/test/resources/testconfigs/broad.json
#     --> sets up the CLI test users and grants the developer-admins email ADMIN access to the cli-test-users SAM group

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [[ $(basename $PWD) != 'terra-cli' ]]; then
  >&2 echo "ERROR: Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi
terra=$PWD/build/install/terra-cli/bin/terra

adminUsersGroupEmail=$1
testConfigFile=$2
if [ -z "$adminUsersGroupEmail" ] || [ -z "$testConfigFile" ]; then
    >&2 echo "ERROR: Usage: ./setup-test-users.sh [adminUsersGroupEmail] [testConfigFile]"
    exit 1
fi

# Fetch test users from config file
declare -a testUsersJson=(`jq -c '[.testUsers[]] | .[]' $2`)

# invite all the test users if the server requires it
echo
echo "Checking whether the server requires inviting new users."
requiresInvite=$($terra config get server --format=json | jq .samInviteRequiresAdmin)

if [[ $requiresInvite == "true" ]]; then
  echo "Inviting test users."
  for userJson in "${testUsersJson[@]}"; do
    userEmail=$(jq -r ".email" <<< "$userJson")
    $terra user status --email="$userEmail" || $terra user invite --email="$userEmail"
  done
else
  echo "Server does not require inviting new users."
fi

# create the cli-test-users SAM group and add it as a user on the spend profile
echo
echo "Creating the SAM group for CLI test users."
groupName="cli-test-users"
$terra group describe --name=$groupName || $terra group create --name=$groupName
groupEmail=$($terra group describe --name=$groupName --format=json | jq -r .email)

echo
echo "Granting the admin users group ADMIN access to the SAM group for CLI test users."
$terra group add-user --name=$groupName --policy=ADMIN --email=$adminUsersGroupEmail

echo
echo "Granting the SAM group USER access to the spend profile."
$terra spend enable --policy=USER --email=$groupEmail

for userJson in "${testUsersJson[@]}"; do
    userEmail=$(jq -r ".email" <<< "$userJson")
    spendEnabled=$(jq -r ".spendEnabled" <<< "$userJson")

    if [[ $spendEnabled == "OWNER" ]]; then
      # owner on both the cli-test-users group and the spend profile resource
      echo
      echo "Adding $userEmail as an ADMIN of the SAM group."
      $terra group add-user --name=$groupName --policy=ADMIN --email=$userEmail
      echo
      echo "Granting $userEmail OWNER access to the spend profile."
      $terra spend enable --policy=OWNER --email=$userEmail

    elif [[ $spendEnabled == "CLI_TEST_USERS_GROUP" ]]; then
      # members of the cli-test-users group
      echo
      echo "Adding $userEmail as a MEMBER of the SAM group."
      $terra group add-user --name=$groupName --policy=MEMBER --email=$userEmail

    elif [[ $spendEnabled == "DIRECTLY" ]]; then
      # users of the spend profile resource
      echo
      echo "Adding $userEmail as a MEMBER of the SAM group."
      $terra spend enable --policy=USER --email=$userEmail

    else
      # no spend profile access
      echo
      echo "do nothing: $userEmail"
    fi
done
