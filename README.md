# terra-cli

1. [Setup and run](#setup-and-run)
2. [Requirements](#requirements)
    * [Authentication](#authentication)
    * [External data](#external-data)
3. [Example usage](#example-usage)
4. [Commands description](#commands-description)
    * [Server](#server)
    * [Workspace](#workspace)
    * [Supported tools](#supported-tools)

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

#### Authentication
1. Use a Google account that is not a Google/Verily corporate account.
2. `terra auth login` launches an OAuth flow that pops out a browser window with a warning login
page ("! Google hasn't verified this app"). This shows up because the CLI is not yet a Google-verified
app. Click through the warnings ("Advanced" -> "Go to ... (unsafe)") to complete the login.

Note: The default server has been temporarily changed from `terra-dev` to `wchamber-dev`.
This is because the Terra dev services are behind a firewall that requires users to be
on the Broad VPN. There is an open ticket with Broad DevOps to address this. In the meantime,
personal development deployments are apparently not behind the firewall, so we're using
one of those as a temporary workaround. If you do have access to the Broad VPN, you can run 
`terra server set terra-dev` to change it back to official Terra dev environment.

#### External data 
To allow supported tools (i.e. the ones shown by `terra app list`) to read or write data external
to the Terra workspace, you need to give the user's pet service account the appropriate access.
To get the email of the user's pet service account, run `terra gcloud config get-value account`.

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
  auth       Commands related to the retrieval and management of user
               credentials.
  server     Commands related to the Terra server.
  workspace  Commands related to the Terra workspace.
  app        Commands related to applications in the Terra workspace context.
```

The `status` command prints details about the current workspace and server.

Each sub-group of commands is described in a sub-section below:
- Authentication
- Server
- Workspace
- App

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

A Terra Workspace is backed by a Google project. Creating a new workspace also creates a new backing Google project.
The same applies to deleting.

The workspace context is tied to the directory on your local machine, similar to how `git` works.
So if you change directories, you lose the workspace context.

#### Supported tools
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