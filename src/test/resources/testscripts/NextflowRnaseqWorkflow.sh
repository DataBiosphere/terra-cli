# Expected usage: NextflowRnaseqSetup.sh {resourceName}
resourceName=$1
if [[ -z "$resourceName" ]]; then
  echo "Expected usage: NextflowRnaseqSetup.sh {resourceName}"
  return 1
fi

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
