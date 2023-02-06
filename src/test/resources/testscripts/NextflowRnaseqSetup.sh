#!/bin/bash
set -e
## This script runs a demo Nextflow workflow on the GLS Pipelines API. The inline comments are talking notes for a demo.
# Expected usage: NextflowRnaseqSetup.sh {resourceName}
resourceName=$1
if [[ -z "$resourceName" ]]; then
  echo "Expected usage: NextflowRnaseqSetup.sh {resourceName}"
  return 1
fi

# [Before demo]
#   - Run through commands once
#       - Do this at least 10 minutes beforehand, so the Nextflow workflow has time to complete
#       - Keep the directory and terminal tab open, so we can switch back to it at the end of the demo, to show a completed workflow
#   - Open the following pages in a browser:
#       - https://github.com/DataBiosphere/terra-cli/releases
#       - https://github.com/nextflow-io/rnaseq-nf
#       - https://cloud.google.com/life-sciences/docs/tutorials/nextflow
#       - file:///Users/marikomedlock/Desktop/working-terra-cli/demo6/results/multiqc_report.html

# Check the status to see the current workspace id and backing GCP project.

terra status

# Nextflow requires a bucket to store temporary files. We can create a controlled resource for this, which means a cloud resource (a bucket in this case) within the backing GCP project.
# Bucket names must be globally unique, so use a random UUID with the dashes removed for the bucket name.
# Terra resource names must only be unique within the workspace, so use a fixed string for the resource name.

bucketName=$(uuidgen | tr "[:upper:]" "[:lower:]" | sed -e 's/-//g')
echo "resourceName: $resourceName, bucketName: $bucketName"
terra resource create gcs-bucket --name=$resourceName --bucket-name=$bucketName
terra resource describe --name=$resourceName

# I will use an example Nextflow workflow from a GitHub repository [show webpage], and checkout a tag that I have tested beforehand. This is the same example workflow that is used on the GCP + Nextflow tutorial [show webpage].

git clone https://github.com/nextflow-io/rnaseq-nf.git
cd rnaseq-nf
git checkout v2.0
cd ..
