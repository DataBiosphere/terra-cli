#!/bin/bash

## This script creates a BigQuery dataset to catalog break-glass requests.
## Running this script more than once with the same project id has no effect (i.e. same as running it once).
## Dependencies: bq
## Inputs: projectId (arg, required)
## Usage: ./tools/create-break-glass-bq.sh [projectId]

## The script assumes that it is being run from the top-level directory "terra-cli/".
if [ $(basename $PWD) != 'terra-cli' ]; then
  echo "Script must be run from top-level directory 'terra-cli/'"
  exit 1
fi

usage="Usage: ./tools/create-break-glass-bq.sh [projectId]"

projectId=$1
if [ -z "$projectId" ]; then
    echo $usage
    exit 1
fi

# keep the dataset/table names here consistent with those in command.workspace.BreakGlass class
dataset=break_glass_requests
table=requests

# create the dataset if it does not exist
bq show $projectId:$dataset || \
    bq --location=US mk -d --description "Break-Glass Requests" $projectId:$dataset

# create the tables if they do not exist
bq show --schema $projectId:$dataset.$table || \
    bq mk --table $projectId:$dataset.$table ./tools/break-glass-bq/tableSchema_requests.json

# list tables in the BQ dataset
bq ls $projectId:$dataset