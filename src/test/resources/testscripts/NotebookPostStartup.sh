#!/bin/bash
set -e
## This script creates a new notebook and echoes the set environment variables

terra status

name=notebook-$RANDOM
terra resource create gcp-notebook --name=$name

# hack to wait for permissions to propagate
until terra notebook start --name=$name
do
    sleep 30
done

terra gcloud compute ssh --quiet --zone us-central1-a --command "source .bash_profile; env" jupyter@$name > notebookPostStartupScript_stdout.txt
cat notebookPostStartupScript_stdout.txt
