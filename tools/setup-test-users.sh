#!/bin/bash

set -e
## This script sets up users for running CLI tests. It only needs to be run once per SAM instance.
## Keep this script in sync with the harness.TestUsers class in the src/test/java directory.
## Dependencies: jq
## Usage: ./setup-test-users.sh

terra=/Users/marikomedlock/Workspaces/terra-cli/build/install/terra-cli/bin/terra

# create the cli-test-users SAM group and add it as a user on the spend profile
echo
echo "Creating the SAM group for CLI test users."
groupName="cli-test-users"
$terra group create --name=$groupName
groupEmail=$($terra group describe --name=$groupName --format=json | jq -r .email)

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