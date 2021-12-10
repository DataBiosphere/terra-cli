# Data and resource references in the Terra CLI

WARNING: this page is a work in progress!

### Workspace context for applications
The Terra CLI defines a workspace context for applications to run in. This context includes:
- `GOOGLE_CLOUD_PROJECT` environment variable set to the backing google project id.
- `GOOGLE_SERVICE_ACCOUNT_EMAIL` environment variable set to the current user's pet SA email in the current workspace.
- Environment variables that are the name of the workspace resources, prefixed with `TERRA_` are set to the resolved
  cloud identifier for those resources (e.g. `mybucket` -> `TERRA_mybucket` set to `gs://mybucket`). Applies to
  referenced and controlled resources.

#### Reference in a CLI command
To use a workspace reference in a Terra CLI command, escape the environment variable to bypass the
shell substitution on the host machine.

Example commands for creating a new controlled bucket resource and then using `gsutil` to get its IAM bindings.
```
> terra resource create gcs-bucket --name=mybucket --bucket_name=mybucket
Successfully added controlled GCS bucket.

> terra gsutil iam get \$TERRA_mybucket
  Setting the gcloud project to the workspace project
  Updated property [core/project].
  
  {
    "bindings": [
      {
        "members": [
          "projectEditor:terra-wsm-dev-e3d8e1f5",
          "projectOwner:terra-wsm-dev-e3d8e1f5"
        ],
        "role": "roles/storage.legacyBucketOwner"
      },
      {
        "members": [
          "projectViewer:terra-wsm-dev-e3d8e1f5"
        ],
        "role": "roles/storage.legacyBucketReader"
      }
    ],
    "etag": "CAE="
  }
```

#### Reference in file
To use a workspace reference in a file or config that will be read by an application, do not escape the
environment variable. Since this will be running inside the Docker container or local process, there is
no need to bypass shell substitution.

Example `nextflow.config` file that includes a reference to a bucket resource in the workspace, the backing
Google project, and the workspace pet SA email.
```
profiles {
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
}
```

#### See all environment variables
Run `terra app execute env` to see all environment variables defined in the Docker container or local process
when applications are launched.

The `terra app execute ...` command is intended for debugging. It lets you execute any command in the Docker
container or local process, not just the ones we've officially supported (i.e. `gsutil`, `bq`, `gcloud`, `nextflow`).
