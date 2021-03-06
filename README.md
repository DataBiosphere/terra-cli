# terra-cli

1. [Install and run](#install-and-run)
    * [Requirements](#requirements)
    * [Login](#login)
    * [Spend profile access](#spend-profile-access)
    * [External data](#external-data)
    * [Troubleshooting](#troubleshooting)
      * [Clear global context](#clear-global-context)
      * [Manual Install](#manual-install)
      * [Manual Uninstall](#manual-uninstall)
2. [Example usage](#example-usage)
3. [Commands description](#commands-description)
    * [Authentication](#authentication)
    * [Server](#server)
    * [Workspace](#workspace)
    * [Resources](#resources)
        * [GCS bucket lifecycle rules](#gcs-bucket-lifecycle-rules)
    * [Data References](#data-references)
    * [Applications](#applications)
    * [Notebooks](#notebooks)
    * [Groups](#groups)
    * [Spend](#spend)
    * [Config](#config)
4. [Workspace context for applications](#workspace-context-for-applications)
    * [Reference in a CLI command](#reference-in-a-cli-command)
    * [Reference in file](#reference-in-file)
    * [See all environment variables](#see-all-environment-variables)
5. [Exit codes](#exit-codes)

-----

### Install and run
To install the latest version:
```
curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash
./terra
```

To install a specific version:
```
export TERRA_CLI_VERSION=0.36.0
curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash
./terra
```

This will install the Terra CLI in the current directory. Afterwards, you may want to add it to your `$PATH` directly
or move it to a place that is already on your `$PATH` (e.g. `/usr/local/bin`).

Re-installing will overwrite any existing installation (i.e. all JARs and scripts will be overwritten), but will not
modify the `$PATH`. So if you have added it to your `$PATH`, that step needs to be repeated after each install.

#### Requirements
1. Java 11
2. Docker 20.10.2 (Must be running)
3. `curl`, `tar`, `gcloud` (For install only)

Note: The CLI doesn't use `gcloud` directly either during install or normal operation.
However, `docker pull` [may use](https://cloud.google.com/container-registry/docs/quickstart#auth) `gcloud` under the 
covers to pull the default Docker image from GCR. This is the reason for the `gcloud` requirement for install.

#### Login
1. `terra auth login` launches an OAuth flow that pops out a browser window with a warning login
page ("! Google hasn't verified this app"). This shows up because the CLI is not yet a Google-verified
app. Click through the warnings ("Advanced" -> "Go to ... (unsafe)") to complete the login.
2. If the machine where you're running the CLI does not have a browser available to it, then use the
manual login flow by setting the browser flag `terra config set browser MANUAL`. See the [Authentication](#authentication)
section below for more details.

#### Spend profile access
In order to spend money (e.g. by creating a project and resources within it) in Terra, you need
access to a billing account via a spend profile. Currently, there is a single spend profile used
by Workspace Manager. Your email needs to either be added as a user of that spend profile or added
to a Terra group that is a user of that spend profile. This needs to be done by someone else with
owner access to that spend profile.

- [Preferred] Add a user to a Terra group that is a user of the spend profile. To also grant permission
to add new members to the group, use `policy=admin` instead.

`terra group add-user --group=enterprise-pilot-testers --policy=member testuser@gmail.com`

- Add a user directly to the spend profile. To also grant permission to add new users to the spend profile,
user `policy=owner` instead.
`terra spend enable --policy=user testuser@gmail.com`

#### External data 
To allow supported applications (i.e. the ones shown by `terra app list`) to read or write data
external to the Terra workspace, you need to give the user's proxy group the appropriate access.
To get the email of the user's proxy group, run `terra auth status`.

#### Troubleshooting
##### Clear global context
Clear the global context file and all credentials. This will then require you to login again.
```
cd $HOME/.terra
rm global-context.json
rm StoredCredential
rm -R pet-keys
```

##### Manual install
A Terra CLI release includes a GitHub release of the `terra-cli` repository and a corresponding Docker image in GCR.
`download-install.sh` is a convenience script that downloads the latest (or specific version) of the install package,
unarchives it, runs the `install.sh` script included inside, and then deletes the install package.

You can also skip the `download-install.sh` script and install manually.
- Download the `terra-cli.tar` install package directly from the 
[GitHub releases page.](https://github.com/DataBiosphere/terra-cli/releases)
- Unarchive the `tar` file.
- Run the install script from the unarchived directory: `./install.sh`

##### Manual uninstall
There is not yet an uninstaller. You can clear the entire global context, which includes the context file, all
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

Create a new Terra workspace (and backing Google project).
Check the current workspace status to confirm it was created successfully.
```
terra workspace create
terra status
```

List all workspaces the user has read access to.
```
terra workspace list
```

If you want to use an existing Terra workspace, use the `set` command instead of `create`.
```
terra workspace set --id=eb0753f9-5c45-46b3-b3b4-80b4c7bea248
```

Run a Nextflow hello world example.
```
terra nextflow run hello
```

Run an [example Nextflow workflow](https://github.com/nextflow-io/rnaseq-nf) in the context of the Terra workspace (i.e.
in the workspace's backing Google project). This is the same example workflow used in the 
[GCLS tutorial](https://cloud.google.com/life-sciences/docs/tutorials/nextflow).
- Fetch the workflow code
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
- Update the `gls` section of the `nextflow.config` file to point to the workspace project and the bucket we just created.
    ```
      gls {
          params.transcriptome = 'gs://rnaseq-nf/data/ggal/transcript.fa'
          params.reads = 'gs://rnaseq-nf/data/ggal/gut_{1,2}.fq'
          params.multiqc = 'gs://rnaseq-nf/multiqc'
          process.executor = 'google-lifesciences'
          process.container = 'nextflow/rnaseq-nf:latest'
          workDir = "$TERRA_mybucket/scratch"
          google.location = 'europe-west2'
          google.region  = 'europe-west1'
          google.project = "$GOOGLE_CLOUD_PROJECT"
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

- Call the Gcloud CLI tools within the workspace context.
This means the commands are executed against the workspace project and as the current user's pet service account.
```
terra gcloud config get-value project
terra gsutil ls
terra bq version
```

- See the list of supported (external) tools.
The CLI runs these tools in a Docker image. Print the image tag that the CLI is currently using.
```
terra app list
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
  group      Manage groups of users.
  gsutil     Call gsutil in the Terra workspace.
  nextflow   Call nextflow in the Terra workspace.
  notebook   Use AI Notebooks in the workspace.
  resolve    Resolve a resource to its cloud id or path.
  resource   Manage resources in the workspace.
  server     Connect to a Terra server.
  spend      Manage spend profiles.
  status     Print details about the current workspace and server.
  version    Get the installed version.
  workspace  Setup a Terra workspace.
```

The `status` command prints details about the current workspace and server.

The `version` command prints the installed version string.

The `gcloud`, `gsutil`, `bq`, and `nextflow` commands call external applications in the context of a Terra workspace.

The other commands are groupings of sub-commands, described in the sections below.
* `auth` [Authentication](#authentication)
* `server` [Server](#server)
* `workspace` [Workspace](#workspace)
* `resource` [Resources](#resources)
* `app` [Applications](#applications)
* `notebook` [Notebooks](#notebooks)
* `group` [Groups](#groups)
* `spend` [Spend](#spend)
* `config` [Config](#config)

#### Authentication
```
Usage: terra auth [COMMAND]
Retrieve and manage user credentials.
Commands:
  login   Authorize the CLI to access Terra APIs and data with user credentials.
  revoke  Revoke credentials from an account.
  status  Print details about the currently authorized account.
```

Only one user can be logged in at a time. To change the active user, revoke the existing credentials and login again.

Login uses the Google OAuth 2.0 installed application [flow](https://developers.google.com/identity/protocols/oauth2/native-app).
If there is a workspace defined in the current context, then logging in also fetches the user's pet SA file for that workspace.

Credentials are part of the global context, so you don't need to login again after switching workspaces.

By default, the CLI opens a browser window for the user to click through the OAuth flow. For some use cases (e.g. CloudShell,
notebook VM), this is not practical because there is no default (or any) browser on the machine. The CLI has a browser
option that controls this behavior. `terra config set browser MANUAL` means the user can copy the url into a browser on a different
machine (e.g. their laptop), confirm the scopes and get the token response, then copy/paste that back into a shell on the
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

The server is part of the global context, so this value applies across workspaces.

#### Workspace
```
Usage: terra workspace [COMMAND]
Setup a Terra workspace.
Commands:
  add-user     Add a user or group to the workspace.
  create       Create a new workspace.
  delete       Delete an existing workspace.
  describe     Describe the workspace.
  list         List all workspaces the current user can access.
  list-users   List the users of the workspace.
  remove-user  Remove a user or group from the workspace.
  set          Set the workspace to an existing one.
  update       Update an existing workspace.
```

A Terra workspace is backed by a Google project. Creating a new workspace also creates a new backing Google 
project. The same applies to deleting.

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
  update                     Update the properties of a resource.
```

A controlled resource is a cloud resource that is managed by Terra. It exists within the current workspace context.
For example, a bucket within the workspace Google project.

A referenced resource is a cloud resource that is NOT managed by Terra. It exists outside the current workspace
context. For example, a BigQuery dataset hosted outside of Terra or in another workspace.

The `check-access` command lets you see whether you have access to a particular resource. This is useful when a
different user created or added the resource and subsequently shared the workspace with you.

The list of resources in a workspace is maintained on the Terra Workspace Manager server. The CLI caches this list
of resources locally, so that external tools (see section below) are not slowed down by round-trips to Workspace 
Manager. The CLI updates the cache on every call to a `terra resource` command. So, if you are working in a shared
workspace, you can run `terra resource list` (for example) to pick up any changes that your collaborators have made.

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

#### Applications
```
Usage: terra app [COMMAND]
Run applications in the workspace.
Commands:
  execute  [FOR DEBUG] Execute a command in the application container for the
             Terra workspace, with no setup.
  list     List the supported applications.
```

The Terra CLI allows running supported external tools within the context of a workspace.
The `app-launch` configuration property controls how tools are run: in a Docker container,
or a local child process.

Nextflow and the Gcloud SDK are the first examples of supported tools.

#### Notebooks
```
Usage: terra notebook [COMMAND]
Use AI Notebooks in the workspace.
Commands:
  start  Start a stopped AI Notebook instance within your workspace.
  stop   Stop a running AI Notebook instance within your workspace.
```

You can create an [AI Platform Notebook](https://cloud.google.com/ai-platform-notebooks) controlled resource with 
`terra resource create ai-notebook`. These `stop`, `start` commands are provided for convenience.

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

The `enterprise-pilot-testers` group is used for managing access to the default spend profile.

#### Spend
```
Usage: terra spend [COMMAND]
Manage spend profiles.
Commands:
  enable      Enable use of the Workspace Manager default spend profile for a
                user or group.
  disable     Disable use of the Workspace Manager default spend profile for a
                user or group.
  list-users  List the users enabled on the Workspace Manager default spend
                profile.
```

These commands allow managing users who are authorized to spend money with Workspace Manager (e.g. by
creating a project and resources within it). A Spend Profile Manager service has not yet been built.
In the meantime, WSM uses a single billing account and manages access to it with a single SAM resource.
These commands are utility wrappers around adding users to this single resource.

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
[image] docker image id = gcr.io/terra-cli-dev/terra-cli/0.50.0:stable
[resource-limit] max number of resources to allow per workspace = 1000

[logging, console] logging level for printing directly to the terminal = OFF
[logging, file] logging level for writing to files in /Users/marikomedlock/.terra/logs = INFO

[server] server = verily-cli
[workspace] workspace = ef8cf0a4-ec70-41be-9fae-9ab6f98cd7e7
```

### Workspace context for applications
The Terra CLI defines a workspace context for applications to run in. This context includes:
- User's pet SA activated as current Google credentials and the `GOOGLE_APPLICATION_CREDENTIALS` environment variable
set to the path to the key file.
- `GOOGLE_CLOUD_PROJECT` environment variable set to the backing google project id.
- Environment variables that are the name of the workspace resources, prefixed with `TERRA_` are set to the resolved
cloud identifier for those resources (e.g. `mybucket` -> `TERRA_mybucket` set to `gs://mybucket`). Applies to 
referenced and controlled resources.

#### Reference in a CLI command
To use a workspace reference in a Terra CLI command, escape the environment variable to bypass the
shell substitution on the host machine.

Example commands for creating a new controlled bucket resource and then using `gsutil` to get its IAM bindings.
```
> terra resource create gcs-bucket --name=mybucket --type=bucket
bucket successfully created: gs://terra-wsm-dev-e3d8e1f5-mybucket
Workspace resource successfully added: mybucket

> terra gsutil iam get \$TERRA_mybucket
  Setting up Terra app environment...
  Activated service account credentials for: [pet-110017243614237806241@terra-wsm-dev-e3d8e1f5.iam.gserviceaccount.com]
  Updated property [core/project].
  Done setting up Terra app environment...
  
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

Example `nextflow.config` file that includes a reference to the backing Google project.
```
profiles {
  gls {
      params.transcriptome = 'gs://rnaseq-nf/data/ggal/transcript.fa'
      params.reads = 'gs://rnaseq-nf/data/ggal/gut_{1,2}.fq'
      params.multiqc = 'gs://rnaseq-nf/multiqc'
      process.executor = 'google-lifesciences'
      process.container = 'nextflow/rnaseq-nf:latest'
      workDir = "$TERRA_mybucket/scratch"
      google.location = 'europe-west2'
      google.region  = 'europe-west1'
      google.project = "$GOOGLE_CLOUD_PROJECT"

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
