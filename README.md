# terra-cli

1. [Install and run](#install-and-run)
    * [Requirements](#requirements)
    * [Login](#login)
    * [Spend profile access](#spend-profile-access)
    * [External data](#external-data)
    * [Local tools installation](#local-tools-installation)
2. [Troubleshooting](#troubleshooting)
    * [Clear context](#clear-context)
    * [Manual Install](#manual-install)
    * [Manual Uninstall](#manual-uninstall)
3. [Example usage](#example-usage)
    * [Nextflow examples](#nextflow-examples)
4. [Commands description](#commands-description)
    * [Applications](#applications)
    * [Authentication](#authentication)
    * [Config](#config)
    * [Git](#git)
    * [Groups](#groups)
    * [gsutil](#gsutil)
    * [Notebooks](#notebooks)
    * [Resources](#resources)
        * [Update A Reference resource](#update-a-reference-resource)
        * [GCS bucket lifecycle rules](#gcs-bucket-lifecycle-rules)
        * [GCS bucket object reference](#gcs-bucket-object-reference)
            * [Reference to a file or folder](#reference-to-a-file-or-folder)
            * [Reference to multiple objects under a folder](#reference-to-multiple-objects-under-a-folder)
        * [Mounting GCS buckets & objects](#mounting-gcs-buckets--objects)
    * [Server](#server)
    * [Spend](#spend)
    * [User](#user)
    * [Workspace](#workspace)
5. [Workspace context for applications](#workspace-context-for-applications)
    * [Reference in a CLI command](#reference-in-a-cli-command)
    * [Reference in file](#reference-in-file)
    * [See all environment variables](#see-all-environment-variables)
    * [Run unsupported tools](#run-unsupported-tools)
    * [Configuring Credentials for AWS Resources](#configuring-credentials-for-aws-resources)
6. [Exit codes](#exit-codes)

-----

## Install and run

To install the latest version:

```shell
curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash && export SUPPRESS_GCLOUD_CREDS_WARNING=true

# Optional: Move to somewhere in PATH
sudo mv terra /usr/local/bin

# Talk to Verily production environment (as opposed to a test environment):
terra server set --name=verily --quiet
```

To install a specific version, set the version as an environment variable

```shell
export TERRA_CLI_VERSION=0.106.0
```

By default, the CLI will be installed without support for Docker (i.e. it won't
pull the Docker image). The TERRA_CLI_DOCKER_MODE environment variable controls
Docker support. Set it to

* DOCKER_NOT_AVAILABLE (default) to skip pulling the Docker image
* DOCKER_AVAILABLE to pull the image (requires Docker to be installed and
  running).

Re-installing will overwrite any existing installation (i.e. all JARs and
scripts will be overwritten), but will not modify the `$PATH`. If you have added
the location of the Terra CLI to your `$PATH`, you will therefore need to add
its location to your path again after each install.

### Requirements

1. Java 17
2. Docker 20.10.2 (Must be running if installing in DOCKER_AVAILABLE mode)
3. `curl`, `tar`, `gcloud` (For install only)

Note: The CLI doesn't use `gcloud` directly either during installation or normal
operation.
However, `docker pull` [may use](https://cloud.google.com/container-registry/docs/quickstart#auth) `gcloud`
under the covers to pull the default Docker image from GCR; therefore, `gcloud`
is required for installation.

### Login

Note: If you are using the CLI on a Terra cloud environment, you do not need to
run the commands below. You are already logged in. You can verify this by
running

```shell
terra auth status
```

* To Launch an OAuth flow that creates a new tab in your browser window where
  you will complete the login
  ```shell
  terra auth login
  ```

* If the machine where you're running the CLI does not have a browser available
  to it, then use the manual login flow by setting the browser flag using. See
  the [Authentication](#authentication) section below for more details.
  ```shell
  terra config set browser MANUAL
  ```

### Spend profile access

In order to spend money in Terra (e.g. by creating a workspace and resources
within it), you need access to a billing account via a spend profile. Currently,
there is a single spend profile used by each team. An admin user can grant you
access. Admins, see [ADMIN.md](https://github.
com/DataBiosphere/terra-cli/blob/main/ADMIN.md#spend) for more details.

### External data

In order to read data from or write data to a private external resource in
Terra, you must grant the appropriate data access permissions to your proxy
group. To view the email address of your proxy group, run `terra auth status`

### Local tools installation

When running `terra app` commands in `LOCAL_PROCESS` `app-launch` mode (the
default), it's necessary to install various tools locally. The following
instructions are for both macOS and Linux.

* `gcloud` - Make sure you have Python installed, then download the .tar.gz
  archive file from the [installation page](https://cloud.google.
  com/sdk/docs/install). Run `gcloud version` to verify the installation.
    - `gcloud builds submit` has `--gcs-bucket-resource` option to specify
      the `--gcs-source-staging-dir` and `--gcs-log-dir`
      options as default.

* `gsutil` - This command is included in
  the [`gcloud` CLI](https://cloud.google.com/sdk/docs/install), or available
  separately [here](https://cloud.google.com/storage/docs/gsutil_install).
  Verify its installation with `gsutil version`
  (also printed as part of `gcloud version`)

* `bq` - This command is included with `gcloud`. More details are available
  [here](https://cloud.google.com/bigquery/docs/bq-command-line-tool).
  Similarly, verify its installation with `bq version`.

* `nextflow` - Install by downloading a `bash` script and running it locally.
  Create a `nextflow` directory somewhere convenient (e.g. `$HOME/nextflow`) and
  switch to it. Then run `curl -s https://get.nextflow.io | bash`. Finally, move
  the `nextflow` executable script to a location on the `$PATH`: the `$PATH` by
  running `sudo mv nextflow /usr/local/bin/`. Verify the installation
  with `nextflow -version`.

* `git` - Follow
  these [instructions](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
  for installing Git on your platform.

Now, these applications are available in `terra` by running, for
example, `terra gsutil ls`. When run with `terra`, environment variables are set
based on resources in the active workspace, and context such as the active GCP
project is set up automatically.

-----

## Troubleshooting

### Clear context

Clear the context file and all credentials. This will require you to login and
select a workspace again.

```shell
cd $HOME/.terra
rm context.json
rm StoredCredential
```

### Manual install

A Terra CLI release includes a GitHub release of the `terra-cli` repository and
a corresponding Docker image in GCR. `download-install.sh` is a convenience
script that downloads the latest (or specific) version of the installation
package, unarchives it, runs the `install.sh` script included inside, and then
deletes the installation package.

You can also skip the `download-install.sh` script and do the installation
manually.

* Download the `terra-cli.tar` install package directly from the
  [GitHub releases page.](https://github.com/DataBiosphere/terra-cli/releases)
* Unarchive the `tar` file.
* Run the installation script from the unarchived directory: `./install.sh`

### Manual uninstall

There is not yet an uninstaller. You can clear the entire context directory,
which includes the context file, all credentials, and all JARs. This will then
require a re-installation (see above).

```shell
rm -R $HOME/.terra
rm /usr/local/bin/terra
```

-----

## Example usage

The commands below walk through a brief demo of the existing commands.

* Fetch the user's credentials. Check the authentication status to confirm the
  login was successful.
  ```shell
  terra auth login
  terra auth status
  ```

* Ping the Terra server.
  ```shell
  terra server status
  ```

* Create a new Terra workspace and backing Google project. Check the current
  context to confirm it was created successfully.
  ```shell
  terra workspace create --id=<my-workspace-id>
  terra status
  ```

* List all workspaces the user has read or write access to.
  ```shell
  terra workspace list
  ```

* If you want to use an existing Terra workspace, use the `set` command instead
  of `create`.
  ```shell
  terra workspace set --id=eb0753f9-5c45-46b3-b3b4-80b4c7bea248
  ```

* Set the Gcloud user and application default credentials.
  ```shell
  gcloud auth login
  gcloud auth application-default login
  ```

### Nextflow Examples

Run a Nextflow hello world example (requires Docker image set and container
running, or Nextflow to be installed locally. For Docker
support, `export TERRA_CLI_DOCKER_MODE=DOCKER_AVAILABLE` before
installing `terra`):

```shell
terra nextflow run hello
```

Run an [example Nextflow workflow](https://github.com/nextflow-io/rnaseq-nf) in
the context of the Terra workspace (i.e. in the workspace's backing Google
project). This is the same example workflow used in the
[GCLS tutorial](https://cloud.google.com/life-sciences/docs/tutorials/nextflow).

* Download the workflow code from GitHub.
  ```shell
  git clone https://github.com/nextflow-io/rnaseq-nf.git
  cd rnaseq-nf
  git checkout v2.0
  cd ..
  ```

* Create a bucket in the workspace for Nextflow to use.
  ```shell
  terra resource create gcs-bucket --name=mybucket --bucket-name=mybucket
  ```

* Update the `gls` section of the `rnaseq-nf/nextflow.config` file to point to
  the workspace project and bucket we just created.
  ```config
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

* Perform a dry-run to confirm the config is set correctly.
  ```shell
  terra nextflow config rnaseq-nf/main.nf -profile gls
  ```

* Kick off the workflow. (This takes about 10 minutes to complete.)
  ```shell
  terra nextflow run rnaseq-nf/main.nf -profile gls
  ```

* To send metrics about the workflow run to a Nextflow Tower server, first
  define an environment variable with the Tower access token. Then specify
  the `-with-tower` flag when kicking off the workflow.
  ```shell
  export TOWER_ACCESS_TOKEN=*****
  terra nextflow run hello -with-tower
  terra nextflow run rnaseq-nf/main.nf -profile gls -with-tower
  ```

* Call the Gcloud CLI tools in the current workspace context. This means that
  Gcloud is configured with the backing Google project and environment variables
  are defined that contain workspace and resource properties (e.g. bucket names,
  pet service account email).
  ```shell
  terra gcloud config get-value project
  terra gsutil ls
  terra bq version
  ```

* See the list of supported third-party tools. The CLI runs these tools in a
  Docker image, if `app-launch` mode is `DOCKER_CONTAINER`. If the
  `app-launch` mode is `LOCAL_PROCESS`, the CLI will assume the tools are
  available in the current shell environment and launch them there.
  ```shell
  terra app list
  ```

* Print the image tag that the CLI is currently using.
  ```shell
  terra config get image
  ```

-----

## Commands description

```
Usage: terra [COMMAND]
Terra CLI
Commands:
  app        Run applications in the workspace.
  auth       Retrieve and manage user credentials.
  bq         Call bq in the Terra workspace.
  config     Configure the CLI.
  cromwell   cromwell Generate a Cromwell configuration file.
  gcloud     Call gcloud in the Terra workspace.
  git        Call git in the Terra workspace.
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

* The `resolve` command is an alias for the `terra resource resolve` command.
* The `status` command prints details about the current workspace and server.
* The `version` command prints the installed version string.
* The `bq`, `gcloud`, `git`, `gsutil` and `nextflow` commands call third-party
  applications in the context of a Terra workspace and are aliases for the
  `terra app [application]` command

The other commands are groupings of sub-commands, described in the sections
below.

* `app` [Applications](#applications)
* `auth` [Authentication](#authentication)
* `config` [Config](#config)
* `cromwell` [Cromwell](#cromwell)
* `git` [Git](#Git)
* `group` [Groups](#groups)
* `gsutil` [gsutil](#gsutil)
* `notebook` [Notebooks](#notebooks)
* `resource` [Resources](#resources)
* `server` [Server](#server)
* `spend` [Spend](#spend)
* `user` [User](#user)
* `workspace` [Workspace](#workspace)

### Applications

```
Usage: terra app [COMMAND]
Run applications in the workspace.
Commands:
  execute  [FOR DEBUG] Execute a command in the application container for the
             Terra workspace, with no setup.
  list     List the supported applications.
```

The Terra CLI allows running supported third-party tools within the context of a
workspace. To see supported tools, run

```shell
terra app list
```

The `app-launch` configuration property controls how tools are run: in a Docker
container, or a local child process.

If you pass `--workspace` flag, it must come immediately after the tool:

```shell
# Works
terra bq --workspace=<workpspace-id> ls

# Doesn't work, --workspace is passed to bq instead of terra
terra bq ls --workspace=<workpspace-id>
```

For creating resources such as BigQuery dataset or GCS bucket, you must create
through terra rather than through tool. This is because terra configures
permissions for you.

```shell
# Works
terra resource create gcs-bucket --name=<resource-name>

# Doesn't work
terra gsutil mb gs://<bucket-name>
```

### Authentication

```
Usage: terra auth [COMMAND]
Retrieve and manage user credentials.
Commands:
  login   Authorize the CLI to access Terra APIs and data with user credentials.
  revoke  Revoke credentials from an account.
  status  Print details about the currently authorized account.
```

Only one user can be logged in at a time. To login as a different user, run

```shell
terra auth login
```

Login uses the Google OAuth 2.0 installed
application [flow](https://developers.google.com/identity/protocols/oauth2/native-app).

You don't need to login again after switching workspaces. You will need to login
again after switching servers, because different Terra deployments may have
different OAuth flows.

By default, the CLI opens a browser window for the user to click through the
OAuth flow. For some use cases (e.g. CloudShell, notebook VM), this is not
practical because there is no default (or any) browser on the machine. The CLI
has a browser option that controls this behavior. The below command displays a
URL, which the user can copy to a browser on a different machine (e.g. their
laptop), complete the login prompt, and then copy/paste the response token back
into a shell on the machine where they want to use the Terra CLI.

```shell
# set the browse option
> terra config set browser MANUAL
Browser launch mode for login is MANUAL (CHANGED).

# auth flow
> terra auth login
Please open the following address in a browser on any machine:
  https://accounts.google.com/o/oauth2/auth?access_type=offline&approval_prompt=force&client_id=[...]
Please enter code: *****
Login successful: testuser@gmail.com
```

### Config

```
Usage: terra config [COMMAND]
Configure the CLI.
Commands:
  get   Get a configuration property value.
  list  List all configuration properties and their values.
  set   Set a configuration property value.
```

These commands are property getters and setters for configuring the Terra CLI.
Currently, the available configuration properties are:

```
OPTION                VALUE                                          DESCRIPTION                                                 
app-launch            DOCKER_CONTAINER                               app launch mode                                             
browser               AUTO                                           browser launch for login                                    
image                 gcr.io/terra-cli-dev/terra-cli/0.246.0:stable  docker image id                                             
resource-limit        1000                                           max number of resources to allow per workspace              
console-logging       OFF                                            logging level for printing directly to the terminal         
file-logging          INFO                                           logging level for writing to files/Users/ginay/.terra/logs  
server                broad-dev-cli-testing                          (unset)                                                     
workspace             (unset)                                        (unset)                                                     
format                TEXT                                           output format 
```

### Cromwell

Utility commands for using
the [Cromwell](https://cromwell.readthedocs.io/en/stable/) workflow engine with
Terra.

```
Usage: terra cromwell [COMMAND]
Commands related to Cromwell workflows.
Commands:
  generate-config  Generate a Cromwell configuration file (cromwell.conf) for use on a Terra workspace cloud environment.
```

To run Cromwell in a notebook instance:

* Generate the config
    ```shell
    terra cromwell generate-config \
        (--workspace-bucket-name=bucket_name | --google-bucket-name=gs://my-bucket) \
        [--dir=my/path]
    ```

* One of `workspace-bucket-name` or `google-bucket-name` is required to specify
  the bucket used by Cromwell for workflow orchestration.
    * `workspace-bucket-name` is a Terra resource name.
    * `google-bucket-name` is a Google Cloud Storage bucket.
      If `google-bucket-name` does not begin with the `gs://` prefix, it will be
      automatically added.

* Start the Cromwell server on `localhost:8000`, run
    ```shell
    java -Dconfig.file=path/to/cromwell.conf -jar cromwell/cromwell-81.jar server
    ```

* In another terminal window, run `cromshell`. Enter `localhost:8000` for
  cromwell server.

* Start workflow through cromshell:
  e.g. `cromshell submit workflow.wdl inputs.json [options.json] [dependencies.zip]`

For more information, see https://github.com/broadinstitute/cromshell.

### Git

```
Usage: terra git [COMMAND]
Call git command in the terra workspace. Besides calling normal Git operation, this command allow cloning git-repo resources in the workspace.
Commands:
  all        Clone all the git-repo resources in the workspace. Usage: terra git clone --all
  resource   Clone specified git-repo resources in the workspace. Usage: terra git clone --resource=<repoResource1Name> --resource=<repoResource2Name>
```

To add a git repo:

```shell
terra resource add-ref git-repo --name=<resource_name> --repo-url=<repo_url>
```

### Groups

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

Terra groups are managed by SAM. These commands are utility wrappers around the
group endpoints.

Say a Terra group's email is `mygroup@mydomain.com`. `name` is `mygroup`,
not `mygroup@mydomain.com`:

```shell
terra group list-users --name=mygroup
```

Adding a member to a Terra group implicitly adds their pet service accounts. For
example, say `terra-user` is added to `mygroup@mydomain.com`. When `mygroup` is
granted access to a resource, `terra-user` is able to access that resource from
any of their Terra workspaces.

### gsutil

You can run `terra gsutil`
or `terra gcloud alpha storage`. `gcloud alpha storage`
is a newer version of `gsutil`. It doesn't support everything, but what it does
support [may be significantly faster](https://stackoverflow.com/collectives/google-cloud/articles/68475140/faster-cloud-storage-transfers-using-the-gcloud-command-line).

### Notebooks

```
Usage: terra notebook [COMMAND]
Use GCP Notebooks in the workspace.
Commands:
  start   Start a stopped Notebook instance within your workspace.
  stop    Stop a running Notebook instance within your workspace.
  launch  Launch a running Notebook instance within your workspace.
```

You can create a notebook (controlled resource) with

```shell
terra resource create [notebook-type] --name=<resourcename> [--workspace=<id>]
```

These `stop`, `start` and `launch` commands are provided for convenience.

* [gcp-notebooks](https://cloud.google.
  com/vertex-ai/docs/workbench/notebook-solution) are supported on workspaces
  created on cloud platform GCP. You can also stop and start the notebook using
  the
  `gcloud notebooks instances [start|stop]` commands.
* [sagemaker-notebooks]() are supported on workspaces created on cloud platform
  AWS. You can also stop and start the notebook using the
  `aws --profile=profile-name sagemaker
  [start-notebook-instance|stop-notebook-instance]` commands.

### Resources

```
Usage: terra resource [COMMAND]
Manage resources in the workspace.
Commands:
  add-ref, add-referenced    Add a new referenced resource.
  check-access               Check if you have access to a referenced resource.
  credentials                Retrieve temporary credentials to access a cloud resource.
  create, create-controlled  Add a new controlled resource.
  delete                     Delete a resource from the workspace.
  describe                   Describe a resource.
  list                       List all resources.
  list-tree                  List all resources and folders in tree view.
  mount                      Mounts all workspace bucket resources.
  open-console               Retrieve console link to access a cloud resource.
  resolve                    Resolve a resource to its cloud id or path.
  unmount                    Unmounts all workspace bucket resources.
  update                     Update the properties of a resource.
```

A controlled resource is a cloud resource that is managed by Terra. It exists
within the current workspace context. For example, a bucket within the workspace
Google project. You can create these with the `create` command.

A referenced resource is a cloud resource that is NOT managed by Terra. It
exists outside the current workspace context. For example, a BigQuery dataset
hosted outside of Terra or in another workspace. You can add these with the
`add-ref` command. The workspace currently supports the following referenced
resource:

* `gcs-bucket`
* `gcs-object`
* `bq-dataset`
* `bq-table`
* `git-repo`

The `check-access` command lets you see whether you have access to a particular
resource. This is useful when a different user created or added the resource and
subsequently shared the workspace with you. `check-access` currently always
returns true for `git-repo` reference type because workspace doesn't support
authentication to external git services yet.

The list of resources in a workspace is maintained on the Terra Workspace
Manager server.

#### Update A Reference resource

User can update the name and description of a reference resource. User can also
update a reference resource to another of the same type. For instance, if a user
creates a reference resource to Bq dataset `foo` and later on wants to point to
Bq dataset `bar` in the same project, one can use the below command to update
the reference. However, one is not allowed to update the reference to a
different type (e.g. update a dataset reference to a data table reference is not
allowed).

```shell
terra resource udpate --name=<fooReferenceName> --new-dataset-id=bar
```

#### GCS bucket lifecycle rules

GCS bucket lifecycle rules are specified by passing a JSON-formatted file path
to the `terra resource create gcs-bucket` command. The expected JSON structure
matches the one used by the `gsutil lifecycle`
[command](https://cloud.google.com/storage/docs/gsutil/commands/lifecycle). This
structure is a subset of the GCS
resource [specification](https://cloud.google.com/storage/docs/json_api/v1/buckets#lifecycle).
Below are some example file contents for specifying a lifecycle rule.

1. Change the storage class to `ARCHIVE` after 10 days.
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

2. Delete any objects with storage class `STANDARD` that were created before
   December 3, 2007.
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

3. Delete any objects that are more than 365 days old.
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

```shell
terra resource create gcs-bucket --name=mybucket --bucket-name=mybucket --auto-delete=365
```

#### GCS bucket object reference

A reference to an GCS bucket object can be created by calling

```shell
terra resource add-ref gcs-object --name=referencename --bucket-name=mybucket --object-name=myobject
```

##### Reference to a file or folder

A file or folder is treated as an object in GCS bucket. By either creating a
folder through the cloud console UI or copying an existing folder of files to
the GCS bucket, a user can create a folder object. So the user can create a
reference to the folder if they have at least `READER` access to the bucket
and/or `READER` access to the folder. Same with a file.

##### Reference to multiple objects under a folder

Different from other referenced resource type, there is also support for
creating a reference to objects in the folder. For instance, a user may create
a `foo/` folder with `bar.txt` and `secret.txt` in it. If the user have at least
READ access to foo/ folder, they have access to anything in the foo/ folder. So
they can add a reference to `foo/bar.txt`, `foo/\*` or `foo/\*.txt`.

> **NOTE** Be careful to provide the correct object name when creating a
> reference. We only check if the user has READER access to the provided path,
> we **do not** check whether the object exists. This is helpful
> because when referencing to foo/\*, it is actually not a real object! So
> a reference to `fooo/` (where object `fooo` does not exist) can be created if
> the user has `READER` access to the bucket or `foo/\*.png` (where there is no
> png files) if they have access to the `foo/` folder.

#### Mounting GCS buckets & objects

Users can mount GCS buckets and referenced folder objects locally to the user's
home directory in `$HOME/workspace/` by running

```shell
terra resource mount
```

Users can specify the `--name` flag with the name of a GCS bucket or GCS object
resource to only mount that individual resource. This flag is useful for
remounting a resource that had failed to mount or has been moved to a different
folder in the workspace.

By default, controlled GCS buckets and referenced folder objects created by the
user will be mounted with read-write permissions while controlled buckets
created by other users and referenced bucket folders will be mounted with
read-only permissions. Users can override this default behavior by specifying
the `--read-only` flag.

```shell
# all mounts to be read-only
terra resource mount --read-only

# all mounts to be read-write
terra resource mount --name=mybucket --read-only=false
```

Users can specify the `--disable-cache` flag. This will disable file metadata
caching and file type caching for objects in the mounted buckets. List
operations such as `ls` will be slower, but will reflect the most up-to-date
state of the bucket. This is useful when working with collaborators in a shared
workspace. See more details in
the [gcsfuse](https://github.com/GoogleCloudPlatform/gcsfuse/blob/master/docs/semantics.md#caching)
repository.

##### Mount Failures

If a mount has failed, an empty directory will be left at mount point with the
resource name and a suffix error string indicating the failure. Users can
remount the bucket after resolving bucket access or bucket reference issues.

Unmounting a single resource can fail if the resource has been renamed or moved
to a different workspace folder. In this case, users can either
run `terra resource unmount` to unmount all mounted resources
in `$HOME/workspace/`. Or, users can directly list out all mounted filesystems
with `mount` and then unmount the resource using its mount path
with `fusermount -u` (for linux) or `umount` for (MacOS).

### Server

```
Usage: terra server [COMMAND]
Connect to a Terra server.
Commands:
  list    List all available Terra servers.
  set     Set the Terra server to connect to.
  status  Print status and details of the Terra server context.
```

A Terra server or environment is a set of connected Terra services (e.g.
Workspace Manager, Data Repo, SAM).

Workspaces exist on a single server, so switching servers will change the list
of workspaces available to you.

### Spend

These commands are intended for admin users. Admins,
see [ADMIN.md](https://github.com/DataBiosphere/terra-cli/blob/main/ADMIN.md#spend)
for more details.

#### User

These user management commands are intended for admin users. Admins,
see [ADMIN.md](https://github.com/DataBiosphere/terra-cli/blob/main/ADMIN.md#users)
for more details.

#### ssh-key

> **Ensure you have the latest CLI version.** To install new CLI version, first
[manually uninstall](#manual-uninstall) the existing CLI and then
[install](#install-and-run) the latest CLI.

`terra user ssh-key` is how Terra does source control in a notebook environment.
It handles the ssh key of the current user. There is one single Terra ssh key
per user in a given server (e.g. broad-dev). With this SSH key, you can perform
source control in a terra-managed notebook instance using git.

To set up an ssh key, add the terra ssh key to your local machine using the
below command

```shell
terra user ssh-key add
```

You should see in the output an ssh public key starting with `ssh-rsa`. Then
copy the public key from the command output and add it to
GitHub. [GitHub's instruction link](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account).

If you think your key is compromised (e.g. the private key on your local machine
is leaked to other user), you must delete the key from your GitHub account and
generate a new Terra ssh key using the below command

```shell
terra user ssh-key generate
```

Once a new key is generated, you need to associate this new key with your GitHub
account
again. [GitHub's instruction link](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account).

### Workspace

```
Usage: terra workspace [COMMAND]
Setup a Terra workspace.
Commands:
  add-user         Add a user or group to the workspace.
  break-glass      Grant break-glass access to a workspace user.
  configure-aws    Generate an AWS configuration file for a workspace.
  duplicate        Duplicate an existing workspace.
  create           Create a new workspace.
  delete           Delete an existing workspace.
  delete-property  Delete the workspace properties.
  describe         Describe the workspace.
  list             List all workspaces the current user can access.
  list-users       List the users of the workspace.
  remove-user      Remove a user or group from the workspace.
  set              Set the workspace to an existing one.
  set-property     Set the workspace properties.
  update           Update an existing workspace.
```

A Terra workspace created on cloud platform GCP is backed by a Google project.
Creating/deleting a workspace also creates/deletes the project.

The `break-glass` command is intended for admin users. Admins,
see [ADMIN.md](https://github.com/DataBiosphere/terra-cli/blob/main/ADMIN.md#break-glass)
for more details.

-----

## Workspace context for applications

The Terra CLI defines a workspace context for applications to run in. This
context includes:

* `GOOGLE_CLOUD_PROJECT` environment variable set to the backing google project
  id.
* `GOOGLE_SERVICE_ACCOUNT_EMAIL` environment variable set to the current user's
  pet SA email in the current workspace.
* Environment variables that are the name of the workspace resources, prefixed
  with `TERRA_` are set to the resolved cloud identifier for those resources (
  e.g. `mybucket` -> `TERRA_mybucket` set to `gs://mybucket`). Applies to
  referenced and controlled resources.

### Reference in a CLI command

To use a workspace reference in a Terra CLI command, escape the environment
variable to bypass the shell substitution on the host machine.

Example commands for creating a new controlled bucket resource and then
using `gsutil` to get its IAM bindings.

```shell
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

### Reference in file

To use a workspace reference in a file or config that will be read by an
application, do not escape the environment variable. Since this will be running
inside the Docker container or local process, there is no need to bypass shell
substitution.

Example `nextflow.config` file that includes a reference to a bucket resource in
the workspace, the backing Google project, and the workspace pet SA email.

```config
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

### See all environment variables

To see all environment variables defined in the Docker container or local
process when applications are launched

```shell
terra app execute env
```

The `terra app execute ...` command is intended for debugging. It lets you
execute any command in the Docker container or local process, not just the ones
we've officially supported (i.e. `gsutil`, `bq`, `gcloud`, `nextflow`).

### Run unsupported tools

To run tools that are not yet supported by the Terra CLI, or to use local
versions of tools, set the `app-launch` configuration property to launch a child
process on the local machine instead of inside a Docker container.

```shell
terra config set app-launch LOCAL_PROCESS
```

Then call the tool with `terra app execute`. Before running the tool command,
the CLI defines environment variables for each workspace resource and
configures `gcloud` with the workspace project. After running the tool command,
the CLI restores the original `gcloud` project configuration.

```shell
terra app execute dsub \
    --provider google-v2 \
    --project \$GOOGLE_CLOUD_PROJECT \
    --regions us-central1 \
    --logging \$TERRA_MY_BUCKET/logging/ \
    --output OUT=\$TERRA_MY_BUCKET/output/out.txt \
    --command 'echo "Hello World" > "${OUT}"' \
    --wait
```

(Note: The command above came from
the `dsub` [README](https://github.com/DataBiosphere/dsub/blob/main/README.md#getting-started-on-google-cloud)
.)

### Configuring Credentials for AWS Resources

Accessing AWS Workspace resources via the AWS CLI or SDK can be configured using
the `terra workspace configure-aws` command. This command writes an
[AWS configuration file](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html)
with two profiles for each AWS resource in the workspace to a file named after
the Workspace in `$HOME/terra/aws/`.

The output of this command is meant to be used with the
[bash `eval` command](https://ss64.com/bash/eval.html) to set up the current
environment to access resource in the current workspace by outputting a bash
command to set the `AWS_CONFIG_FILE` environment variable to the newly created
AWS configuration file. In turn this file contains two profiles for each
resource, one named after the resource, and one suffixed with `-ro`; the former
provides write/read access to the resource, the latter provides read-only.

#### AWS Configuration Example

Workspace has a single AWS S3 Storage folder (output truncated):

```shell
> terra resource describe --name aws_folder_20230422
Name:         aws_folder_20230422
Description:  My First Storage Folder
Type:         AWS_S3_STORAGE_FOLDER
Region:       us-east-1
...
AWS S3 Storage Folder: s3://v0-saas-devel-us-east-1-terra/aws_folder_20230422/
# Objects: 0
```

Call `terra workspace configure-aws` wrapped in a bash `eval` command and note
that environment variable `AWS_CONFIG_FILE` points at the newly written config
file:

``` shell
> eval "$(terra workspace configure-aws)"
> echo $AWS_CONFIG_FILE 
/Users/jczerk/.terra/aws/verily_devel/jczerk_aws_202304131028.conf
```

Now we can use profile `aws_folder_20230422` to copy a file into our S3 Storage
Folder:

``` shell
> aws --profile=aws_folder_20230422 s3 cp /tmp/hello.txt s3://v0-saas-devel-us-east-1-terra/aws_folder_20230422/
upload: /tmp/hello.txt to s3://v0-saas-devel-us-east-1-terra/aws_folder_20230422/hello.txt
```

And using read-only profile `aws_folder_20230422-ro` allows us to list this
file:

```shell
> aws --profile=aws_folder_20230422-ro s3 ls s3://v0-saas-devel-us-east-1-terra/aws_folder_20230422/
2023-04-22 09:53:15          0 
2023-05-01 11:32:10         14 hello.txt
```

But not delete it:

```shell
> aws --profile=aws_folder_20230422-ro s3 rm s3://v0-saas-devel-us-east-1-terra/aws_folder_20230422/hello.txt
delete failed: s3://v0-saas-devel-us-east-1-terra/aws_folder_20230422/hello.txt An error occurred (AccessDenied) when calling the DeleteObject operation: Access Denied
```

Switching back to profile `aws_folder_20230422`, delete succeeds:

```shell
> aws --profile=aws_folder_20230422 s3 rm s3://v0-saas-devel-us-east-1-terra/aws_folder_20230422/hello.txt
delete: s3://v0-saas-devel-us-east-1-terra/aws_folder_20230422/hello.txt
```

Caching credentials using
tool [`aws-vault`](https://github.com/99designs/aws-vault) is recommended, and
can be configured using options ` --cache-with-aws-vault` and
`--aws-vault-path`.

-----

## Exit codes

The CLI sets the process exit code as follows.

* 0 = Successful program execution
* 1 = User-actionable error (e.g. missing parameter, workspace not defined in
  the current context)
* 2 = System or internal error (e.g. error making a request to a Terra service)
* 3 = Unexpected error (e.g. null pointer exception)

App exit codes will be passed through to the caller. e.g.
If `gcloud --malformedOption` returns exit code `2`, then
`terra gcloud --malformedOption` will also return exit code `2`.
