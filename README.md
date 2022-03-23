# terra-cli

1. [Install and run](#install-and-run)
    * [Requirements](#requirements)
    * [Login](#login)
    * [Spend profile access](#spend-profile-access)
    * [External data](#external-data)
    * [Local Tools Installation](#local-tools-installation)
    * [Troubleshooting](#troubleshooting)
      * [Clear context](#clear-context)
      * [Manual Install](#manual-install)
      * [Manual Uninstall](#manual-uninstall)
2. [Example usage](#example-usage)
3. [Commands description](#commands-description)
    * [Applications](#applications)
    * [Authentication](#authentication)
    * [Config](#config)
    * [Git](#git)
    * [Groups](#groups)
    * [gsutil](#gsutil)
    * [Notebooks](#notebooks)
    * [Resources](#resources)
    * [Server](#server)
    * [Spend](#spend)
    * [User](#user)
    * [Workspace](#workspace)
    * [Resources](#resources)
        * [GCS bucket lifecycle rules](#gcs-bucket-lifecycle-rules)
        * [GCS bucket object reference](#gcs-bucket-object-reference)
          * [Reference to a file or folder](#reference-to-a-file-or-folder)
          * [Reference to multiple objects under a folder](#reference-to-multiple-objects-under-a-folder)
        * [Update A Reference resource](#update-a-reference-resource)
4. [Workspace context for applications](#workspace-context-for-applications)
    * [Reference in a CLI command](#reference-in-a-cli-command)
    * [Reference in file](#reference-in-file)
    * [See all environment variables](#see-all-environment-variables)
5. [Exit codes](#exit-codes)

-----

### Install and run
To install the latest version:
```
curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash && export SUPPRESS_GCLOUD_CREDS_WARNING=true

# Optional: Move to somewhere in PATH
sudo mv terra /usr/local/bin

# Talk to Verily production environment (as opposed to a test environment):
terra server set --name=verily --quiet
```

To install a specific version, run before above commands: `export TERRA_CLI_VERSION=0.106.0`

By default, the CLI will be installed without support for Docker (i.e. it won't pull the Docker image).
The TERRA_CLI_DOCKER_MODE environment variable controls Docker support. Set it to
* DOCKER_NOT_AVAILABLE (default) to skip pulling the Docker image
* DOCKER_AVAILABLE to pull the image (requires Docker to be installed and running).

Re-installing will overwrite any existing installation (i.e. all JARs and scripts will be overwritten), but will not
modify the `$PATH`. So if you have added it to your `$PATH`, that step needs to be repeated after each install.

#### Requirements
1. Java 11
2. Docker 20.10.2 (Must be running if installing in DOCKER_AVAILABLE mode)
3. `curl`, `tar`, `gcloud` (For install only)

Note: The CLI doesn't use `gcloud` directly either during install or normal operation.
However, `docker pull` [may use](https://cloud.google.com/container-registry/docs/quickstart#auth) `gcloud` under the 
covers to pull the default Docker image from GCR. This is the reason for the `gcloud` requirement for install.

#### Login
1. `terra auth login` launches an OAuth flow that pops out a browser window to complete the login.
2. If the machine where you're running the CLI does not have a browser available to it, then use the
manual login flow by setting the browser flag `terra config set browser MANUAL`. See the [Authentication](#authentication)
section below for more details.

#### Spend profile access
In order to spend money (e.g. by creating a workspace and resources within it) in Terra, you need
access to a billing account via a spend profile. Currently, there is a single spend profile used
by Workspace Manager. An admin user can grant you access.
Admins, see [ADMIN.md](https://github.com/DataBiosphere/terra-cli/blob/main/ADMIN.md#spend) for more details.

#### External data 
In order to read or write external data from Terra, you should grant data access to your proxy group.
`terra auth status` shows the email of your proxy group.

#### Local Tools Installation
When running `terra app` commands in `LOCAL_PROCESS` `app-launch` mode (the default),
it's necessary to install the various tools locally. The following instructions are
for MacOS or Linux.
- `gcloud` - Make sure you have Python installed, then download the .tar.gz archive file from the [installation page](https://cloud.google.com/sdk/docs/install). Run `gcloud version` to verify the installation.
- `gsutil` - included in the [`gcloud` CLI](https://cloud.google.com/sdk/docs/install), or available separately [here](https://cloud.google.com/storage/docs/gsutil_install).
Verify the installation with `gsutil version` (also printed as part of `gcloud version`)
- `bq` - included with `gcloud`. More details are available [here](https://cloud.google.com/bigquery/docs/bq-command-line-tool).
Similarly, verify the installation with `bq version`.
- `nextflow` - Install by downloading a `bash` script and running it locally. Create a `nextflow` directory
somewhere convenient (e.g. $HOME/nextflow) and switch to it. Then do `curl -s https://get.nextflow.io | bash`.
Finally, move the `nextflow` executable script to a location on the `$PATH`: `sudo mv nextflow /usr/local/bin/`.
Verify the installation with `nextflow -version`.
- `git` - Follow [instruction](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) here for installing Git on different platform. 

Now, these applications are available in `terra` by doing, for example, `terra gsutil ls`. When
run in `terra`, environment variables are set based on resources in the active workspace, and
context such as the active GCP project is set up automatically. 
#### Troubleshooting
##### Clear context
Clear the context file and all credentials. This will require you to login and select a workspace again.
```
cd $HOME/.terra
rm context.json
rm StoredCredential
```

##### Manual install
A Terra CLI release includes a GitHub release of the `terra-cli` repository and a corresponding Docker image in GCR.
`download-install.sh` is a convenience script that downloads the latest (or specific) version of the install package,
unarchives it, runs the `install.sh` script included inside, and then deletes the install package.

You can also skip the `download-install.sh` script and do the install manually.
- Download the `terra-cli.tar` install package directly from the 
[GitHub releases page.](https://github.com/DataBiosphere/terra-cli/releases)
- Unarchive the `tar` file.
- Run the install script from the unarchived directory: `./install.sh`

##### Manual uninstall
There is not yet an uninstaller. You can clear the entire context directory, which includes the context file, all
credentials, and all JARs. This will then require a re-install (see above).
```
rm -R $HOME/.terra
```

### Example usage
The commands below walk through a brief demo of the existing commands.

Fetch the user's credentials.
Check the authentication status to confirm the login was successful.
```
terra auth login
terra auth status
```

Ping the Terra server.
```
terra server status
```

Create a new Terra workspace and backing Google project.
Check the current context to confirm it was created successfully.
```
terra workspace create
terra status
```

List all workspaces the user has read or write access to.
```
terra workspace list
```

If you want to use an existing Terra workspace, use the `set` command instead of `create`.
```
terra workspace set --id=eb0753f9-5c45-46b3-b3b4-80b4c7bea248
```

Set the Gcloud user and application default credentials.
```
gcloud auth login
gcloud auth application-default login
```

#### Nextflow Examples
Run a Nextflow hello world example (requires Docker image set and container running, or Nextflow to
be installed locally. For Docker support, `export TERRA_CLI_DOCKER_MODE=DOCKER_AVAILABLE` before installing `terra`):
```
terra nextflow run hello
```

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
The CLI runs these tools in a Docker image, if `app-launch` mode is `DOCKER_CONTAINER`. If the
`app-launch` mode is `LOCAL_PROCESS`, the CLI will assume the tools are available in the current
shell environment and launch them there.
    ```
    terra app list
    ```
- Print the image tag that the CLI is currently using.
    ```
    terra config get image
    ```

### Commands description
```
Usage: terra [COMMAND]
Terra CLI
Commands:
  app        Run applications in the workspace.
  auth       Retrieve and manage user credentials.
  bq         Call bq in the Terra workspace.
  config     Configure the CLI.
  gcloud     Call gcloud in the Terra workspace.
  git        Call git in the Tera workspace
  group      Manage groups of users.
  gsutil     Call gsutil in the Terra workspace.
  nextflow   Call nextflow in the Terra workspace.
  notebook   Use GCP Notebooks in the workspace.
  resolve    Resolve a resource to its cloud id or path.
  resource   Manage resources in the workspace.
  server     Connect to a Terra server.
  spend      Manage spend profiles.
  status     Print details about the current workspace and server.
  user       Manage users.
  version    Get the installed version.
  workspace  Setup a Terra workspace.
```

The `status` command prints details about the current workspace and server.

The `version` command prints the installed version string.

The `gcloud`, `git`, `gsutil`, `bq`, and `nextflow` commands call third-party applications in the context of a Terra workspace.

The `resolve` command is an alias for the `terra resource resolve` command.

The other commands are groupings of sub-commands, described in the sections below.
* `app` [Applications](#applications)
* `auth` [Authentication](#authentication)
* `config` [Config](#config)
* `git` [Git](#Git)
* `group` [Groups](#groups)
* `gsutil` [gsutil](#gsutil)
* `notebook` [Notebooks](#notebooks)
* `resource` [Resources](#resources)
* `server` [Server](#server)
* `spend` [Spend](#spend)
* `user` [User](#user)
* `workspace` [Workspace](#workspace)

#### Applications
```
Usage: terra app [COMMAND]
Run applications in the workspace.
Commands:
  execute  [FOR DEBUG] Execute a command in the application container for the
             Terra workspace, with no setup.
  list     List the supported applications.
```

The Terra CLI allows running supported third-party tools within the context of a workspace.
To see supported tools, run `terra app list`.

The `app-launch` configuration property controls how tools are run: in a Docker container,
or a local child process.

If you pass `--workspace` flag, it must come immediately after the tool:

```
# Works
> terra bq --workspace=<workpspace-id> ls

# Doesn't work, --workspace is passed to bq instead of terra
> terra bq ls --workspace=<workpspace-id>
```

For creating resources such as BigQuery dataset or GCS bucket, you must create through terra rather
than through tool. This is because terra configures permissions for you.

```
# Works
> terra resource create gcs-bucket --name=<resource-name>

# Doesn't work
> terra gsutil mb gs://<bucket-name>
```

#### Authentication
```
Usage: terra auth [COMMAND]
Retrieve and manage user credentials.
Commands:
  login   Authorize the CLI to access Terra APIs and data with user credentials.
  revoke  Revoke credentials from an account.
  status  Print details about the currently authorized account.
```

Only one user can be logged in at a time. Call `terra auth login` to login as a different user.

Login uses the Google OAuth 2.0 installed application [flow](https://developers.google.com/identity/protocols/oauth2/native-app).

You don't need to login again after switching workspaces. You will need to login again after switching servers, because
different Terra deployments may have different OAuth flows.

By default, the CLI opens a browser window for the user to click through the OAuth flow. For some use cases (e.g. CloudShell,
notebook VM), this is not practical because there is no default (or any) browser on the machine. The CLI has a browser
option that controls this behavior. `terra config set browser MANUAL` means the user can copy the URL into a browser on a different
machine (e.g. their laptop), complete the login prompt, and then copy/paste the response token back into a shell on the
machine where they want to use the Terra CLI. Example usage:
```
> terra config set browser MANUAL
Browser launch mode for login is MANUAL (CHANGED).

> terra auth login
Please open the following address in a browser on any machine:
  https://accounts.google.com/o/oauth2/auth?access_type=offline&approval_prompt=force&client_id=[...]
Please enter code: *****
Login successful: testuser@gmail.com
```

#### Config
```
Usage: terra config [COMMAND]
Configure the CLI.
Commands:
  get   Get a configuration property value.
  list  List all configuration properties and their values.
  set   Set a configuration property value.
```

These commands are property getters and setters for configuring the Terra CLI. Currently the available
configuration properties are:
```
[app-launch] app launch mode = DOCKER_CONTAINER
[browser] browser launch for login = AUTO
[image] docker image id = gcr.io/terra-cli-dev/terra-cli/0.118.0:stable
[resource-limit] max number of resources to allow per workspace = 1000

[logging, console] logging level for printing directly to the terminal = OFF
[logging, file] logging level for writing to files in /Users/jaycarlton/.terra/logs = INFO

[server] server = broad-dev-cli-testing
[workspace] workspace = (unset)
[format] output format = TEXT
```
#### Git
```
Usage: terra git [COMMAND]
Call git command in the terra workspace. Besides calling normal Git operation, this command allow cloning git-repo resources in the workspace.
Commands:
  all        Clone all the git-repo resources in the workspace. Usage: terra git clone --all
  resource   Clone specified git-repo resources in the workspace. Usage: terra git clone --resource=<repoResource1Name> --resource=<repoResource2Name>
```
To add a git repo:
```
> terra resource add-ref git-repo --name=<resource_name> --repo-url=<repo_url>
```
#### Groups
```
Usage: terra group [COMMAND]
Manage groups of users.
Commands:
  add-user     Add a user to a group with a given policy.
  create       Create a new Terra group.
  delete       Delete an existing Terra group.
  describe     Describe the group.
  list         List the groups to which the current user belongs.
  list-users   List the users in a group.
  remove-user  Remove a user from a group with a given policy.
```

Terra groups are managed by SAM. These commands are utility wrappers around the group endpoints.

Say a Terra group's email is `mygroup@mydomain.com`. `name` is `mygroup`, not `mygroup@mydomain.com`: 

```
> terra group list-users --name=mygroup
```

#### gsutil

You can run `terra gsutil` or `terra gcloud alpha storage`. `gcloud alpha storage`
is a newer version of `gsutil`. It doesn't support everything, but what it does
support [may be significantly faster](https://stackoverflow.com/collectives/google-cloud/articles/68475140/faster-cloud-storage-transfers-using-the-gcloud-command-line).

#### Notebooks
```
Usage: terra notebook [COMMAND]
Use GCP Notebooks in the workspace.
Commands:
  start  Start a stopped GCP Notebook instance within your workspace.
  stop   Stop a running GCP Notebook instance within your workspace.
```

You can create a [GCP Notebook](https://cloud.google.com/vertex-ai/docs/workbench/notebook-solution) controlled
resource with `terra resource create gcp-notebook --name=<resourcename> [--workspace=<id>]`. These `stop`, `start`
commands are provided for convenience. You can also stop and start the notebook using the `gcloud notebooks instances
start/stop` commands.

#### Resources
```
Usage: terra resource [COMMAND]
Manage resources in the workspace.
Commands:
  add-ref, add-referenced    Add a new referenced resource.
  check-access               Check if you have access to a referenced resource.
  create, create-controlled  Add a new controlled resource.
  delete                     Delete a resource from the workspace.
  describe                   Describe a resource.
  list                       List all resources.
  resolve                    Resolve a resource to its cloud id or path.
  update                     Update the properties of a resouce
```

A controlled resource is a cloud resource that is managed by Terra. It exists within the current workspace context.
For example, a bucket within the workspace Google project. You can create these with the `create` command.

A referenced resource is a cloud resource that is NOT managed by Terra. It exists outside the current workspace
context. For example, a BigQuery dataset hosted outside of Terra or in another workspace. You can add these with the
`add-ref` command. The workspace currently supports the following referenced resource: 
- `gcs-bucket`
- `gcs-object`
- `bq-dataset`
- `bq-table`
- `git-repo`

The `check-access` command lets you see whether you have access to a particular resource. This is useful when a
different user created or added the resource and subsequently shared the workspace with you. `check-access` currently always 
returns true for `git-repo` reference type because workspace doesn't support authentication to external git services yet.

The list of resources in a workspace is maintained on the Terra Workspace Manager server. The CLI caches this list
of resources locally. Third-party tools can access resource details via environment variables (e.g. $TERRA_mybucket
holds the `gs://` URL of the workspace bucket resource named `mybucket`). The CLI updates the cache on every call to
a `terra resource` command. So, if you are working in a shared workspace, you can run `terra resource list` (for
example) to pick up any changes that your collaborators have made.

##### GCS bucket lifecycle rules
GCS bucket lifecycle rules are specified by passing a JSON-formatted file path to the
`terra resource create gcs-bucket` command. The expected JSON structure matches the one used by the `gsutil lifecycle` 
[command](https://cloud.google.com/storage/docs/gsutil/commands/lifecycle). This structure is a subset of the GCS
resource [specification](https://cloud.google.com/storage/docs/json_api/v1/buckets#lifecycle). Below are some
example file contents for specifying a lifecycle rule.

(1) Change the storage class to `ARCHIVE` after 10 days.
```json
{
  "rule": [
    {
      "action": {
        "type": "SetStorageClass",
        "storageClass": "ARCHIVE"
      },
      "condition": {
      	"age": 10
      }
    }
  ]
}
```

(2) Delete any objects with storage class `STANDARD` that were created before December 3, 2007.
```json
{
  "rule": [
    {
      "action": {
        "type": "Delete"
      },
      "condition": {
      	"createdBefore": "2007-12-03",
        "matchesStorageClass": [
          "STANDARD"
        ]
      }
    }
  ]
}
```

(3) Delete any objects that are more than 365 days old.
```json
{
  "rule": [
    {
      "action": {
        "type": "Delete"
      },
      "condition": {
      	"age": 365
      }
    }
  ]
}
```
There is also a command shortcut for specifying this type of lifecycle rule (3).
```
terra resource create gcs-bucket --name=mybucket --bucket-name=mybucket --auto-delete=365
```

##### GCS bucket object reference
A reference to an GCS bucket object can be created by calling
```
terra resource add-ref gcs-object --name=referencename --bucket-name=mybucket --object-name=myobject
```

###### Reference to a file or folder
A file or folder is treated as an object in GCS bucket. By either creating a folder
through the cloud console UI or copying an existing folder of files to the GCS
bucket, a user can create a folder object. So the user can create a reference to
the folder if they have at least `READER` access to the bucket and/or `READER` access to
the folder. Same with a file. 

###### Reference to multiple objects under a folder
Different from other referenced resource type, there is also support for
creating a reference to objects in the folder. For instance, a user may create a
a `foo/` folder with `bar.txt` and `secret.txt` in it. If the user have at least READ
access to foo/ folder, they have access to anything in the foo/ folder. So
they can add a reference to `foo/bar.txt`, `foo/\*` or `foo/\*.txt`. 

> **NOTE** Be careful to provide the correct object name when creating a
> reference. We only check if the user has READER access to the provided path, 
> we **do not** check whether the object exists. This is helpful
> because when referencing to foo/\*, it is actually not a real object! So
> a reference to `fooo/` (where object `fooo` does not exist) can be created if
> the user has `READER` access to the bucket or `foo/\*.png` (where there is no
> png files) if they have access to the `foo/` folder.

##### Update A Reference resource
User can update the name and description of a reference resource. User can also
update a reference resource to another of the same type. For instance, if a user 
creates a reference resource to Bq dataset `foo` and later on wants to point to
Bq dataset `bar` in the same project, one can use 
`terra resource udpate --name=<fooReferenceName> --new-dataset-id=bar` to update
the reference. However, one is not allowed to update the reference to a
different type (e.g. update a dataset reference to a data table reference is not
allowed).

#### Server
```
Usage: terra server [COMMAND]
Connect to a Terra server.
Commands:
  list    List all available Terra servers.
  set     Set the Terra server to connect to.
  status  Print status and details of the Terra server context.
```

A Terra server or environment is a set of connected Terra services (e.g. Workspace Manager, Data Repo, SAM).

Workspaces exist on a single server, so switching servers will change the list of workspaces available to you.

#### Spend
These commands are intended for admin users.
Admins, see [ADMIN.md](https://github.com/DataBiosphere/terra-cli/blob/main/ADMIN.md#spend) for more details.

#### User
These commands are intended for admin users.
Admins, see [ADMIN.md](https://github.com/DataBiosphere/terra-cli/blob/main/ADMIN.md#users) for more details.

#### Workspace
```
Usage: terra workspace [COMMAND]
Setup a Terra workspace.
Commands:
  add-user     Add a user or group to the workspace.
  break-glass  Grant break-glass access to a workspace user.
  clone        Clone an existing workspace.
  create       Create a new workspace.
  delete       Delete an existing workspace.
  describe     Describe the workspace.
  list         List all workspaces the current user can access.
  list-users   List the users of the workspace.
  remove-user  Remove a user or group from the workspace.
  set          Set the workspace to an existing one.
  update       Update an existing workspace.
```

A Terra workspace is backed by a Google project. Creating/deleting a workspace also creates/deletes the project.

The `break-glass` command is intended for admin users.
Admins, see [ADMIN.md](https://github.com/DataBiosphere/terra-cli/blob/main/ADMIN.md#break-glass) for more details.

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

#### Run unsupported tools
To run tools that are not yet supported by the Terra CLI, or to use local versions of tools, set the `app-launch`
configuration property to launch a child process on the local machine instead of inside a Docker container.
```
terra config set app-launch LOCAL_PROCESS
```

Then call the tool with `terra app execute`. Before running the tool command, the CLI defines environment variables
for each workspace resource and configures `gcloud` with the workspace project. After running the tool command, the
CLI restores the original `gcloud` project configuration.
```
terra app execute dsub \
    --provider google-v2 \
    --project \$GOOGLE_CLOUD_PROJECT \
    --regions us-central1 \
    --logging \$TERRA_MY_BUCKET/logging/ \
    --output OUT=\$TERRA_MY_BUCKET/output/out.txt \
    --command 'echo "Hello World" > "${OUT}"' \
    --wait
```
(Note: The command above came from the `dsub` [README](https://github.com/DataBiosphere/dsub/blob/main/README.md#getting-started-on-google-cloud).)

### Exit codes
The CLI sets the process exit code as follows.

- 0 = Successful program execution
- 1 = User-actionable error (e.g. missing parameter, workspace not defined in the current context)
- 2 = System or internal error (e.g. error making a request to a Terra service)
- 3 = Unexpected error (e.g. null pointer exception)

App exit codes will be passed through to the caller. e.g. If `gcloud --malformedOption` returns exit code `2`, then
`terra gcloud --malformedOption` will also return exit code `2`.
