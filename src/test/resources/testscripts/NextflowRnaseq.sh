#!/bin/bash
set -e
## This script runs a demo Nextflow workflow on the GLS Pipelines API. The inline comments are talking notes for a demo.

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

resourceName="terraclitesting"
bucketName=$(uuidgen | tr "[:upper:]" "[:lower:]" | sed -e 's/-//g')
echo "resourceName: $resourceName, bucketName: $bucketName"
terra resource create gcs-bucket --name=$resourceName --bucket-name=$bucketName
terra resource list
# Wait for permissions to propagate on the new bucket before attempting to use it
# TODO(PF-2333): It may be possible to lower this time in the future, but currently the users pet regularly does not
#  have access by 15 minutes post-waiting.
sleep 1200

# I will use an example Nextflow workflow from a GitHub repository [show webpage], and checkout a tag that I have tested beforehand. This is the same example workflow that is used on the GCP + Nextflow tutorial [show webpage].

git clone https://github.com/nextflow-io/rnaseq-nf.git
cd rnaseq-nf
git checkout v2.0
cd ..

# Nextflow users modify the nextflow.config file to specify where to run the batch jobs. I will modify the gls section because I will run against the Google Life Sciences Pipelines API.
terra nextflow -version

# Note the hard-coded 'network' and 'subnetwork' strings are the names of the VPC network and subnetwork, respectively, that replace the default ones in Terra projects. You can find these in the Cloud Console, under the VPC network section.
#[open rnaseq-nf/nextflow.config in a text editor, update the gls section]
#      workDir = "$TERRA_MYBUCKET/scratch"
#      google.location = 'us-central1'
#      google.project = "$GOOGLE_CLOUD_PROJECT"
#      google.region  = 'us-central1'
#      google.lifeSciences.serviceAccountEmail = "$GOOGLE_SERVICE_ACCOUNT_EMAIL"
#      google.lifeSciences.network = 'network'
#      google.lifeSciences.subnetwork = 'subnetwork'
mv rnaseq-nf/nextflow.config rnaseq-nf/nextflow.config_original
sed "s\
;      workDir = 'gs://rnaseq-nf/scratch' // <- replace with your own bucket\!\
;      workDir = \"\$TERRA_$resourceName/scratch\"\
\n      google.project = \"\$GOOGLE_CLOUD_PROJECT\"\
\n      google.lifeSciences.serviceAccountEmail = \"\$GOOGLE_SERVICE_ACCOUNT_EMAIL\"\
\n      google.lifeSciences.network = 'network'\
\n      google.lifeSciences.subnetwork = 'subnetwork'\
;" rnaseq-nf/nextflow.config_original > rnaseq-nf/nextflow.config_sedpass1

sed "s\
;      google.region  = 'europe-west2'\
;      google.region = 'us-central1'\
\n      google.location = 'us-central1'\
;" rnaseq-nf/nextflow.config_sedpass1 > rnaseq-nf/nextflow.config

# Now we can do a dry-run of the Nextflow workflow to confirm that the config file was correctly modified.

terra nextflow config rnaseq-nf/main.nf -profile gls

# And kick off the actual Nextflow workflow.

terra nextflow run rnaseq-nf/main.nf -profile gls

# This will take about 10 minutes to complete. [Switch tabs] I started this workflow in a different workspace earlier and here is the resulting HTML report [show local webpage].
