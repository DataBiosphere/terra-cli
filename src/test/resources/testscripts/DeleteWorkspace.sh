#!/bin/bash
set -e
## This script deletes the current workspace after running an integration test.

terra status

# delete any resources
terra resources list --format=json > resources.json
jq -c '.[]' resources.json | while read i; do
  name=$(echo $i | jq -r '.name');
  echo "deleting workspace resource $name"
  terra resources delete --name=$name;
done

# delete the workspace
terra workspace delete
