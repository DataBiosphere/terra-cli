#!/bin/bash

set -e
## This script sets up users for running CLI tests. It only needs to be run once per SAM instance.
## Keep this script in sync with the harness.TestUsers class in the src/test/java directory.
##
## The admin users group email argument for this script should be the email address of a SAM group that contains
## several admin emails (e.g. developer-admins group on the dev SAM deployment at the Broad contains the corporate
## emails of all PF team developers as of Sept 23, 2021). This is to prevent the team from losing access if the
## person who originally ran this script is not available.
##
## Dependencies: jq
## Inputs: adminUsersGroupEmail (arg, required) email address of the SAM group for admin users
## Usage: ./setup-test-users.sh  developer-admins@dev.test.firecloud.org
#     --> sets up the CLI test users and grants the developer-admins email ADMIN access to the cli-test-users SAM group

usage="Usage: ./setup-test-users.sh [adminUsersGroupEmail]"

adminUsersGroupEmail=$1
if [ -z "$adminUsersGroupEmail" ]; then
    echo $usage
    exit 1
fi

terra=/Users/marikomedlock/Workspaces/terra-cli/build/install/terra-cli/bin/terra

# create the cli-test-users SAM group and add it as a user on the spend profile
echo
echo "Creating the SAM group for CLI test users."
groupName="cli-test-users"
$terra group create --name=$groupName
groupEmail=$($terra group describe --name=$groupName --format=json | jq -r .email)

echo
echo "Granting the admin users group ADMIN access to the SAM group for CLI test users."
$terra group add-user --name=$groupName --policy=ADMIN --email=$adminUsersGroupEmail

echo
echo "Granting the SAM group USER access to the spend profile."
$terra spend enable --policy=USER --email=$groupEmail

# penelope is an owner on both the cli-test-users group and the spend profile resource
echo
echo "Adding Penelope.TwilightsHammer as an ADMIN of the SAM group."
$terra group add-user --name=$groupName --policy=ADMIN --email=Penelope.TwilightsHammer@test.firecloud.org

echo
echo "Granting Penelope.TwilightsHammer OWNER access to the spend profile."
$terra spend enable --policy=OWNER --email=Penelope.TwilightsHammer@test.firecloud.org

# john and lily are members of the cli-test-users group
echo
echo "Adding John.Whiteclaw as a MEMBER of the SAM group."
$terra group add-user --name=$groupName --policy=MEMBER --email=John.Whiteclaw@test.firecloud.org
echo
echo "Adding Lily.Shadowmoon as a MEMBER of the SAM group."
$terra group add-user --name=$groupName --policy=MEMBER --email=Lily.Shadowmoon@test.firecloud.org

# brooklyn and noah are users of the spend profile resource
echo
echo "Granting Brooklyn.Thunderlord USER access to the spend profile."
$terra spend enable --policy=USER --email=Brooklyn.Thunderlord@test.firecloud.org
echo
echo "Granting Noah.Frostwolf USER access to the spend profile."
$terra spend enable --policy=USER --email=Noah.Frostwolf@test.firecloud.org

# ethan has no spend profile access
# do nothing: Ethan.Bonechewer@test.firecloud.org
