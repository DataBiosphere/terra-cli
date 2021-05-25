#!/bin/bash
set -e
## This script runs a demo Nextflow workflow on the GLS Pipelines API. The inline comments are talking notes for a demo.

# Currently, we're connected to the Terra development server, and there is no current workspace defined.

terra status

# Now we can create a workspace. This will prompt a login. The scary-looking login screen is temporary, until we get our CLI app approved by Google.

terra workspace create

# [While that's running] This calls WSM to get a new Terra workspace. WSM in turn calls RBS to get a buffered GCP project, because creating a new project from scratch takes a while. The next part of this demo will run Nextflow against this GCP project. There were some short-term modifications we had to make to the buffered project configuration to support Nextflow.

# [If that's still running] Nextflow expects the default VPC network and default Compute Engine service account to be defined. The "right" solution here is to modify the Nextflow codebase to allow specifying a non-default VPC network and Compute Engine service account. But for the alpha demo/release, it was more expedient to just modify the project configuration, and plan to make Nextflow code changes down the line.

# Now the workspace was created. Check the status again to see the workspace id and backing GCP project.

terra status

# Nextflow requires a bucket to store temporary files. We can create a controlled resource for this, which means a cloud resource (a bucket in this case) within the backing GCP project.
# Bucket names must be globally unique, so use a random UUID with the dashes removed for the bucket name.
# Terra resource names must only be unique within the workspace, so use a fixed string for the resource name.

bucketName=$(uuidgen | sed -e 's/-//g')
terra resources create gcs-bucket --name=terraclitesting --bucket-name=$bucketName
terra resources list

# I will use an example Nextflow workflow from a GitHub repository [show webpage], and checkout a tag that I have tested beforehand. This is the same example workflow that is used on the GCP + Nextflow tutorial [show webpage].

git clone https://github.com/nextflow-io/rnaseq-nf.git
cd rnaseq-nf
git checkout v2.0
cd ..

# Nextflow users modify the nextflow.config file to specify where to run the batch jobs. I will modify the gls section because I will run against the Google Life Sciences Pipelines API.
terra nextflow -version

#[open rnaseq-nf/nextflow.config in a text editor, update the gls section]
#      workDir = "$TERRA_MYBUCKET/scratch"
#      google.location = 'europe-west2'
#      google.project = "$GOOGLE_CLOUD_PROJECT"
#      google.region  = 'europe-west1'
mv rnaseq-nf/nextflow.config rnaseq-nf/nextflow.config_original
sed "s\
;      workDir = 'gs://rnaseq-nf/scratch' // <- replace with your own bucket\!\
;      workDir = \"\$TERRA_$resourceName/scratch\"\
\n      google.location = 'europe-west2'\
\n      google.project = \"\$GOOGLE_CLOUD_PROJECT\"\
;" rnaseq-nf/nextflow.config_original > rnaseq-nf/nextflow.config

# Now we can do a dry-run of the Nextflow workflow to confirm that the config file was correctly modified.

terra nextflow config rnaseq-nf/main.nf -profile gls

# And kick off the actual Nextflow workflow.

terra nextflow run rnaseq-nf/main.nf -profile gls

# This will take about 10 minutes to complete. [Switch tabs] I started this workflow in a different workspace earlier and here is the resulting HTML report [show local webpage].
