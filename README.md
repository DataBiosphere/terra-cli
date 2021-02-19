# terra-cli

1. [Setup and run](#setup-and-run)
2. [Requirements](#requirements)
    * [Login](#login)
    * [Spend profile access](#spend-profile-access)
    * [External data](#external-data)
3. [Example usage](#example-usage)
4. [Commands description](#commands-description)
    * [Authentication](#authentication)
    * [Server](#server)
    * [Workspace](#workspace)
    * [Resources](#resources)
    * [Applications](#applications)
    * [Groups](#groups)
    * [Spend](#spend)
    * [Supported tools](#supported-tools)
5. [Workspace context for applications](#workspace-context-for-applications)
    * [Reference in a CLI command](#reference-in-a-cli-command)
    * [Reference in file](#reference-in-file)
    * [See all environment variables](#see-all-environment-variables)

-----

### Setup and run
From the top-level directory.
```
source tools/local-dev.sh
terra
```

### Requirements
1. Java 11
2. Docker 20.10.2 (Must be running)

#### Login
1. Use a Google account that is not a Google/Verily corporate account.
2. `terra auth login` launches an OAuth flow that pops out a browser window with a warning login
page ("! Google hasn't verified this app"). This shows up because the CLI is not yet a Google-verified
app. Click through the warnings ("Advanced" -> "Go to ... (unsafe)") to complete the login.

#### Spend profile access
In order to spend money (e.g. by creating a project and resources within it) in Terra, you need
access to a billing account via a spend profile. Currently, there is a single spend profile used
by Workspace Manager. Your email needs to either be added as a user of that spend profile or added
to a Terra group that is a user of that spend profile. This needs to be done by someone else with
owner access to that spend profile.

- [Preferred] Add a user to a Terra group that is a user of the spend profile. To also grant permission
to add new members to the group, use `policy=admin` instead.

`terra groups add-user --group=enterprise-pilot-testers --policy=member mmdevverily4@gmail.com`

- Add a user directly to the spend profile. To also grant permission to add new users to the spend profile,
user `policy=owner` instead.
`terra spend enable --policy=user mmdevverily@gmail.com`

#### External data 
To allow supported applications (i.e. the ones shown by `terra app list`) to read or write data
external to the Terra workspace, you need to give the user's pet service account the appropriate
access. To get the email of the user's pet service account, run `terra gcloud config get-value account`.

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

Create an empty directory and change into it.
Create a new Terra workspace (and backing Google project).
Check the current workspace status to confirm it was created successfully.
```
mkdir new-workspace; cd new-workspace
terra workspace create
terra status
```

If you wanted to use an existing Terra workspace, use the mount command instead of create.
```
terra workspace mount eb0753f9-5c45-46b3-b3b4-80b4c7bea248
```

Call the Gcloud CLI tools within the workspace context.
This means the commands are executed against the workspace project and as the current user.
```
terra gcloud config get-value project
terra gsutil ls
terra bq version
```

Enable the Nextflow tool. This will create a sub-directory called `nextflow`.
Run a Nextflow hello world example.
```
terra app enable nextflow
terra nextflow run hello
```

See the list of supported (external) tools.
The CLI runs these tools in a Docker image. Print the image tag that the CLI is currently using.
```
terra app list
terra app get-image
```

### Commands description
```
Usage: terra [COMMAND]
Terra CLI
Commands:
  status     Print details about the current workspace.
  auth       Retrieve and manage user credentials.
  server     Connect to a Terra server.
  workspace  Setup a Terra workspace.
  resources  Manage controlled resources in the workspace.
  app        Run applications in the workspace.
  notebooks  Use AI Notebooks in the workspace.
  groups     Manage groups of users.
  spend      Manage spend profiles.
```

The `status` command prints details about the current workspace and server.

Each sub-group of commands is described in a sub-section below:
- Authentication
- Server
- Workspace
- Resources
- Applications
- Groups
- Spend

#### Authentication
```
Usage: terra auth [COMMAND]
Commands related to the retrieval and management of user credentials.
Commands:
  status  Print details about the currently authorized account.
  login   Authorize the CLI to access Terra APIs and data with user credentials.
  revoke  Revoke credentials from an account.
```

Only one user can be logged in at a time. To change the active user, revoke the existing credentials and login again.

Login uses the Google OAuth 2.0 installed application [flow](https://developers.google.com/identity/protocols/oauth2/native-app).
If there is a workspace defined in the current context, then logging in also fetches the user's pet SA file for that workspace.

Credentials are part of the global context, so you don't need to login again after switching workspaces.

#### Server
```
Usage: terra server [COMMAND]
Commands related to the Terra server.
Commands:
  status  Print status and details of the Terra server context.
  list    List all available Terra servers.
  set     Set the Terra server to connect to.
```

A Terra server or environment is a set of connected Terra services (e.g. Workspace Manager, Data Repo, SAM).

The server is part of the global context, so this value applies across workspaces.

#### Workspace
```
Usage: terra workspace [COMMAND]
Commands related to the Terra workspace.
Commands:
  create       Create a new workspace.
  mount        Mount an existing workspace to the current directory.
  delete       Delete an existing workspace.
  list-users   List the users of the workspace.
  add-user     Add a user to the workspace.
  remove-user  Remove a user from the workspace.
```

A Terra workspace is backed by a Google project. Creating a new workspace also creates a new backing Google 
project. The same applies to deleting.

The workspace context is tied to the directory on your local machine, similar to how `git` works.
So if you change directories, you lose the workspace context.

#### Resources
```
Usage: terra resources [COMMAND]
Manage controlled resources in the workspace.
Commands:
  create    Create a new controlled resource.
  delete    Delete an existing controlled resource.
  describe  Describe an existing controlled resource.
  list      List all controlled resources.
```

A controlled resource is a cloud resource managed by the Terra workspace on behalf of the user.
Currently, the only supported controlled resource is a bucket.

#### Applications
```
Usage: terra app [COMMAND]
Commands related to applications in the Terra workspace context.
Commands:
  list       List the supported applications.
  get-image  Get the Docker image used for launching applications.
  set-image  Set the Docker image to use for launching applications.
  execute    [FOR DEBUG] Execute a command in the application container for the
               Terra workspace, with no setup.
```

The Terra CLI allows running supported external tools within the context of a workspace.
Nextflow and the Gcloud CLIs are the first examples of supported tools.
Exactly what it means to be a "supported" tool is still under discussion.

#### Groups
```
Usage: terra groups [COMMAND]
Manage groups of users.
Commands:
  list         List the groups to which the current user belongs.
  create       Create a new Terra group.
  delete       Delete an existing Terra group.
  describe     Print the group email address.
  list-users   List the users in a group.
  add-user     Add a user to a group.
  remove-user  Remove a user from a group.
```

Terra groups are managed by SAM. These commands are utility wrappers around the group endpoints.

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

These commands allow managing the users authorized to spend money with Workspace Manager (e.g. by
creating a project and resources within it). A Spend Profile Manager service has not yet been built.
In the meantime, WSM uses a single billing account and manages access to it with a single SAM resource.
These commands are utility wrappers around adding users to this single resource.

### Workspace context for applications
The Terra CLI defines a workspace context for applications to run in. This context includes:
- User's pet SA activated as current Google credentials and path to the key file passed in
via `GOOGLE_APPLICATION_CREDENTIALS` environment variable.
- Backing google project id passed in via `TERRA_GOOGLE_PROJECT_ID` environment variable.
- Workspace references to controlled cloud resources resolved in an environment variable that is the name 
of the workspace reference, all in uppercase, with `TERRA_` prefixed. (e.g. `my_bucket` -> `TERRA_MY_BUCKET`).
- In the future, it will also include references to external cloud resources (e.g. a bucket outside the workspace).

#### Reference in a CLI command
To use a workspace reference in a Terra CLI command, escape the environment variable to bypass the
shell substitution on the host machine.

Example commands for creating a new controlled bucket resource and then using `gsutil` to get its IAM bindings.
```
> terra resources create --name=my_bucket --type=bucket
bucket successfully created: gs://terra-wsm-dev-e3d8e1f5-my_bucket
Workspace resource successfully added: my_bucket

> terra gsutil iam get \${TERRA_MY_BUCKET}
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
environment variable. Since this will be running inside the Docker container, there is no need to escape it.

Example `nextflow.config` file that includes a reference to the backing Google project.
```
profiles {
  gls {
      params.transcriptome = 'gs://rnaseq-nf/data/ggal/transcript.fa'
      params.reads = 'gs://rnaseq-nf/data/ggal/gut_{1,2}.fq'
      params.multiqc = 'gs://rnaseq-nf/multiqc'
      process.executor = 'google-lifesciences'
      process.container = 'nextflow/rnaseq-nf:latest'
      google.project = ${TERRA_GOOGLE_PROJECT_ID}
      google.region  = 'europe-west2'
  }
}
```

Example commands for creating a new controlled bucket resource and then running a Nextflow workflow using
this bucket as the working directory.
```
> terra resources create --name=my_bucket --type=bucket
bucket successfully created: gs://terra-wsm-dev-e3d8e1f5-my_bucket
Workspace resource successfully added: my_bucket

> terra nextflow run rnaseq-nf/main.nf -profile gls -work-dir \${TERRA_MY_BUCKET}
  Setting up Terra app environment...
  Activated service account credentials for: [pet-110017243614237806241@terra-wsm-dev-e3d8e1f5.iam.gserviceaccount.com]
  Updated property [core/project].
  Done setting up Terra app environment...
[...Nextflow output...]
```

#### See all environment variables
Run `terra app execute env` to see all environment variables defined in the Docker container where applications
are run.

The `terra app execute ...` command is intended for debugging and lets you execute any command in the Docker
container, not just the ones we've officially "supported" (i.e. gsutil, bq, gcloud, nextflow).
