# Using Nextflow with the Terra CLI

WARNING: this page is a work in progress!

Run an [example Nextflow workflow](https://github.com/nextflow-io/rnaseq-nf) in the context of the Terra workspace (i.e.
in the workspace's backing Google project). This is the same example workflow used in the
[GCLS tutorial](https://cloud.google.com/life-sciences/docs/tutorials/nextflow).
- Download the workflow code from GitHub.
    ```
    git clone https://github.com/nextflow-io/rnaseq-nf.git
    cd rnaseq-nf
    git checkout v2.0
    cd ..
    ```
- Create a bucket in the workspace for Nextflow to use.
    ```
    terra resource create gcs-bucket --name=mybucket --bucket-name=mybucket
    ```
- Update the `gls` section of the `rnaseq-nf/nextflow.config` file to point to the workspace project and bucket
  we just created.
    ```
      gls {
          params.transcriptome = 'gs://rnaseq-nf/data/ggal/transcript.fa'
          params.reads = 'gs://rnaseq-nf/data/ggal/gut_{1,2}.fq'
          params.multiqc = 'gs://rnaseq-nf/multiqc'
          process.executor = 'google-lifesciences'
          process.container = 'nextflow/rnaseq-nf:latest'
          workDir = "$TERRA_mybucket/scratch"

          google.region  = 'us-east1'
          google.project = "$GOOGLE_CLOUD_PROJECT"

          google.lifeSciences.serviceAccountEmail = "$GOOGLE_SERVICE_ACCOUNT_EMAIL"
          google.lifeSciences.network = 'network'
          google.lifeSciences.subnetwork = 'subnetwork'
      }
    ```
- Do a dry-run to confirm the config is set correctly.
    ```
    terra nextflow config rnaseq-nf/main.nf -profile gls
    ```
- Kick off the workflow. (This takes about 10 minutes to complete.)
    ```
    terra nextflow run rnaseq-nf/main.nf -profile gls
    ```

- To send metrics about the workflow run to a Nextflow Tower server, first define an environment variable with the Tower
  access token. Then specify the `-with-tower` flag when kicking off the workflow.
    ```
    export TOWER_ACCESS_TOKEN=*****
    terra nextflow run hello -with-tower
    terra nextflow run rnaseq-nf/main.nf -profile gls -with-tower
    ```

- Call the Gcloud CLI tools in the current workspace context.
  This means that Gcloud is configured with the backing Google project and environment variables are defined that
  contain workspace and resource properties (e.g. bucket names, pet service account email).
```
terra gcloud config get-value project
terra gsutil ls
terra bq version
```

- See the list of supported third-party tools.
  The CLI runs these tools in a Docker image. Print the image tag that the CLI is currently using.
```
terra app list
terra config get image
```
