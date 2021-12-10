
The Terra command-line interface (CLI) makes it easy to interact with Terra workspaces and resources interactively or from programmatic environments (e.g. Terra notebooks or shell scripts).

# Installation
To install the latest version:
```
curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash
./terra
```

To install a specific version:
```
export TERRA_CLI_VERSION=0.106.0
curl -L https://github.com/DataBiosphere/terra-cli/releases/latest/download/download-install.sh | bash
./terra
```

This will install the Terra CLI in the current directory. Afterwards, you may want to add it to your `$PATH` directly
or move it to a place that is already on your `$PATH` (e.g. `/usr/local/bin`).

Re-installing will overwrite any existing installation (i.e. all JARs and scripts will be overwritten), but will not
modify the `$PATH`. So if you have added it to your `$PATH`, that step needs to be repeated after each install.

`terra auth login` launches an OAuth flow that pops out a browser window to complete the login.

# Requirements
1. Java 11
2. Docker 20.10.2 (Must be running)
3. `curl`, `tar`, `gcloud` (For install only)

Note: The CLI doesn't use `gcloud` directly either during install or normal operation.
However, `docker pull` [may use](https://cloud.google.com/container-registry/docs/quickstart#auth) `gcloud` under the 
covers to pull the default Docker image from GCR. This is the reason for the `gcloud` requirement for install.

# Command reference

See the [Terra CLI command docs](/docs/commands/terra.adoc).

## Billing access
In order to spend money (e.g. by creating a workspace and resources within it) in Terra, you need
access to a billing account via a spend profile. Currently, there is a single spend profile used
by Workspace Manager. An admin user can grant you access.
Admins, see [ADMIN.md](https://github.com/DataBiosphere/terra-cli/blob/main/ADMIN.md#spend) for more details.

## Troubleshooting
### Clear global context
Clear the global context file and all credentials. This will require you to login and select a workspace again.
```
cd $HOME/.terra
rm global-context.json
rm StoredCredential
rm -R pet-keys
```

### Manual install
A Terra CLI release includes a GitHub release of the `terra-cli` repository and a corresponding Docker image in GCR.
`download-install.sh` is a convenience script that downloads the latest (or specific) version of the install package,
unarchives it, runs the `install.sh` script included inside, and then deletes the install package.

You can also skip the `download-install.sh` script and do the install manually.
- Download the `terra-cli.tar` install package directly from the 
[GitHub releases page.](https://github.com/DataBiosphere/terra-cli/releases)
- Unarchive the `tar` file.
- Run the install script from the unarchived directory: `./install.sh`

### Manual uninstall
There is not yet an uninstaller. You can clear the entire context directory, which includes the context file, all
credentials, and all JARs. This will then require a re-install (see above).
```
rm -R $HOME/.terra
```


### Running unsupported tools
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
