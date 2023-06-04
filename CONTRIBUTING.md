# terra-cli

1. [Setup development environment](#setup-development-environment)
    * [Dependencies](#dependencies)
    * [Logging](#logging)
    * [Troubleshooting](#troubleshooting)
2. [Publish a release](#publish-a-release)
3. [Testing](#testing)
    * [Requirements](#requirements)
    * [Two types of tests](#two-types-of-tests)
    * [Run tests](#run-tests)
    * [Override default test config](#override-default-test-config)
        * [Override default server](#override-default-server)
        * [Override default Docker image](#override-default-docker-image)
        * [Override context directory](#override-context-directory)
    * [Setup test users](#setup-test-users)
    * [Automated tests](#automated-tests)
    * [Troubleshooting](#troubleshooting-1)
    * [Debugging tips](#debugging-tips)
4. [Docker](#docker)
    * [Pull an existing image](#pull-an-existing-image)
    * [Build a new image](#build-a-new-image)
    * [Publish a new image](#publish-a-new-image)
    * [Update the default image](#update-the-default-image)
5. [Code](#code)
    * [Code structure](#code-structure)
    * [Supported tools](#supported-tools)
        * [Adding a new supported tool](#add-a-new-supported-tool)
    * [Business logic](#business-logic)
    * [Commands](#commands)
    * [Serialization](#serialization)
    * [Terra and cloud services](#terra-and-cloud-services)
    * [Servers](#servers)
    * [Workspace & Resource Ids](#workspace--resource-ids)
    * [Adding a new resource type](#adding-a-new-resource-type)
6. [Command style guide](#command-style-guide)
    * [Options instead of parameters](#options-instead-of-parameters)
    * [Always specify a description](#always-specify-a-description)
    * [Alphabetize command lists](#alphabetize-command-lists)
    * [User readable exception messages](#user-readable-exception-messages)
    * [Singular command group names](#singular-command-group-names)
7. [Auth overview](#auth-overview)

-----

## Setup development environment

The TERRA_CLI_DOCKER_MODE environment variable controls Docker support. Set it
to DOCKER_NOT_AVAILABLE (default) to skip pulling the Docker image or
DOCKER_AVAILABLE to pull the image (requires Docker to be installed and running)

* From the top-level directory, run:
  ```shell
  source tools/local-dev.sh
  ```

* To rebuild after changing code:
  ```shell
  ./gradlew installDist
  ```

### Dependencies

We
use [Gradle's dependency locking](https://docs.gradle.org/current/userguide/dependency_locking.html)
to ensure that builds use the same transitive dependencies, so they're
reproducible. This means that adding or updating a dependency requires telling
Gradle to save the change. If you're getting errors that mention "dependency
lock state" after changing a dep, you need to do this step.

To update version dependencies:

```shell 
./gradlew dependencies --write-locks
```

### Logging

Logging is turned off by default. Modify the level with
the `terra config set logging` command. Available levels are listed in the
command usage.

### Troubleshooting

* Wipe the global context directory.
  ```shell 
  rm -R $HOME/.terra
  ```
* Re-run the setup script.
  ```shell
  source tools/local-dev.sh
  ```

-----

## Publish a release

A release includes a GitHub release of the `terra-cli` repository and a
corresponding Docker image pushed to GCR.

The GitHub action that runs on-merge to the `main` branch automatically builds
the code and creates a GitHub release. So, this section about publishing a
release manually is only intended for testing the release process, releasing a
fix before code is merged (e.g. as a GitHub pre-release), or debugging errors in
the GitHub action.

To publish a release manually, from the current local code:

1. Create a tag (e.g. `test123`) and push it to the remote repository. The tag
   should not include any uppercase letters.
    ```shell
    git tag -a test123 -m "testing version 123"
    git push --tags
    ```

2. Update the version in `settings.gradle`.
    ```
    gradle.ext.cliVersion = 'test123'
    ```

3. Login to GitHub and run the `tools/publish-release.sh` script. This will
   publish a pre-release, which does not affect the "Latest release" tag.
    ```shell
    gh auth login
   ./tools/publish-release.sh test123
    ```
   To publish a regular release, add `true` as a second argument.
     ```shell
    gh auth login
    ./tools/publish-release.sh test123 true
    ```

Note that GitHub automatically attaches an archive of the source code to the
release. If you have local changes that are not yet committed, then they may not
be reflected in the source code archive, but they will be included in the
installation package. We don't use the source code archive for install.

Three shell scripts are published for users.

* `download-install.sh`: This is convenience script that:
    * Downloads the latest (or specific version) of the installation package
    * Unarchives it
    * Runs the `install.sh` script included inside
    * Deletes the installation package

  It is published as a separate file in each GitHub release. The intent is to
  have a one-line install command  `curl -L download-install.sh | bash`. Note
  that this installs the CLI in the current directory. Afterwards, the user can
  optionally add it to their `$PATH`.

* `install.sh`: This is an installer script that:
    * Moves all the JARs to `$HOME/.terra/lib`
    * Moves the `terra` run script and the `README.md` file outside the
      unarchived install package directory
    * Deletes the unarchived install package directory
    * Sets the Docker image id to the default
    * Pulls the default Docker image id

          It is included in the `terra-cli.tar` install package in each GitHub
          release. It needs to be run from the same directory: `./install.sh`

* `terra`: This is the run script that wraps the Java call to the CLI.
    * It looks for the JARs on the classpath in the `$HOME/.terra/lib`
      directory.
    * This script is generated by the Gradle application plugin, so any changes
      should be made there.

  It is included in the `terra-cli.tar` install package in each GitHub release.
  This is the script users can add to their `$PATH` to invoke the CLI more
  easily from another directory.

-----

## Testing

### Requirements

The tests require the Docker daemon to be running (install mode
DOCKER_AVAILABLE).

### Two types of tests

There are two types of CLI tests:

* Unit tests call commands directly in Java. They run against source code; no
  CLI installation is required. By default, unit tests will run in parallel on
  up to half the available cores in order to run faster. This behavior is
  controlled by the `maxParallelForks` setting in `build.gradle`. Example unit
  test code:
  ```
  // `terra auth status --format=json`
  TestCommand.Result cmd = TestCommand.runCommand("auth", "status", "--format=json");
  ```

* Integration tests call commands from a bash script run in a separate process.
  They run against a CLI installation, either one built directly from source
  code via `./gradlew install` or one built from the latest GitHub release.
  Integration tests often use passthrough apps (e.g. nextflow, gcloud, gsutil,
  bq, etc.) which maintain their own global state, so by default we do not run
  them in parallel to avoid clobbering state across runners. Example integration
  test code:
  ```
  // run a script that includes a Nextflow workflow
  int exitCode = new TestBashScript().runScript("NextflowRnaseq.sh");
  ```

While it's possible to mix both types of testing in the same JUnit method, that
should be avoided because then the test is running commands against two
different versions of the code (the source code directly in the same process as
the test, and the installed code in a separate process from the test). This
could be confusing to track down errors.

Both types of tests:

* Use the same code to authenticate a test user without requiring browser
  interaction.
* Override the context directory to `build/test-context/`, so that tests don't
  overwrite the context for an existing CLI installation on the same machine.
  Unit tests additionally create subdirectories inside `build/test-context/`
  based on the runner number, so each process's logs will live in a directory
  like `build/test-context/1`

### Run tests

* Run unit tests (TestTag `unit`) directly against the source code:
  ```shell
  ./gradlew runTestsWithTag -PtestTag=unit`
  ```
* Run integration tests (TestTag `integration`) against an installation built
  from source code:
  ```shell
  ./gradlew runTestsWithTag -PtestTag=integration
  ```
* Run integration tests against an installation built from the latest GitHub
  release:
  ```shell
  ./gradlew runTestsWithTag -PtestTag=integration -PtestInstallFromGitHub
  ```
* Run a single test by specifying the `--tests` option:
  ```shell
  ./gradlew runTestsWithTag -PtestTag=unit --tests Workspace.createFailsWithoutSpendAccess
  ```
* Suppress console display of the test command's stdIn & stdOut by specifying
  the `-PquietConsole` option
  ```shell
  `./gradlew runTestsWithTag -PtestTag=unit -PquietConsole`
  ```
* By default, tests are run against all cloud platforms supported by the CLI
  server. Add the platform to target tests for a single platform
  ```shell
  ./gradlew runTestsWithTag -PtestTag=unit -Pplatform=gcp
  ```

### Override default test config

#### Override default server

The tests run against the `broad-dev` server by default. You can run them
against a different server by specifying the Gradle `server` property. e.g.:

```shell
./gradlew runTestsWithTag -PtestTag=unit -Pserver=broad-dev-cli-testing -PtestConfig=broad
```

#### Override default Docker image

The tests use the default Docker image by default. This is the image in GCR that
corresponds the current version in
`build.gradle`. This default image does not include any changes to the `docker/`
directory that have not yet been released. You can run the tests with a
different Docker image by specifying the Gradle `dockerImage` property. e.g.:

```shell
./gradlew runTestsWithTag -PtestTag=unit -PdockerImage=terra-cli/local:7094e3f
```

The on-PR-push GitHub action uses this flag to run against a locally built image
if there are any changes to the `docker/` directory.

#### Override context directory

The `.terra` context directory is stored in the user's home directory (`$HOME`)
by default. You can override this default by setting the Gradle `contextDir`
property to a valid directory. e.g.:

```shell
./gradlew runTestsWithTag -PtestTag=unit -PcontextDir=$HOME/context/for/tests
```

If the property does not point to a valid directory, then the CLI will throw
a `SystemException`.

This option is intended for tests, so that they don't overwrite the context for
an installation on the same machine.

Note that this override does not apply to installed JAR dependencies. So if you
run integration tests against a CLI installation built from the latest GitHub
release, the dependent libraries will overwrite an existing `$HOME/.terra/lib`
directory, though logs, credentials, and context files will be written to the
specified directory. (This doesn't apply to unit tests or integration tests run
against a CLI installation built directly from source code, because their
dependent libraries are all in the Gradle build directory.)

You can also override the context directory in normal operation by specifying
the `TERRA_CONTEXT_PARENT_DIR`
environment variable. This can be helpful for debugging without clobbering an
existing installation. e.g.:

```shell
export TERRA_CONTEXT_PARENT_DIR="/Desktop/cli-testing"
terra config list
```

### Setup test users

Tests use domain-wide delegation (i.e. Harry Potter users). This avoids the
Google OAuth flow, which requires interacting with a browser. Before running
tests against a Terra server, the test users need to be setup there. Setup
happens exclusively in SAM, so if there are multiple Terra servers that all talk
to the same SAM instance, then you only need to do this setup once.

The CLI uses the test users defined in test config (eg `testconfig/broad.json`).
This includes:

* Have permission to use the default WSM spend profile via the `cli-test-users`
  SAM group.
* Have permission to use the default WSM spend profile directly on the SAM
  resource.
* Do not have permission to use the default WSM spend profile.

You can see the available test users on the users
admin [page](https://admin.google.com/ac/users) with a
`test.firecloud.org` GSuite account.

The script to setup the initial set of test users on the SAM dev instance is
in `tools/setup-test-users.sh`. Note that the current testing setup uses
pre-defined users in the `test.firecloud.org` domain. There would be some
refactoring involved in varying this domain.

Note that the script takes an ADMIN group email as a required argument. This
should be the email address of a SAM group that contains several admin emails (
e.g. developer-admins group on the dev SAM deployment at the Broad contains the
corporate emails of all PF team developers as of Sept 23, 2021). This is to
prevent the team from losing access if the person who originally ran this script
is not available.

If the current server requires users to be invited before they can register,
then the user who runs this script must be an admin user (i.e. a member of
the `fc-admins` Google group in the SAM GSuite). The script invites all the test
users if they do not already exist in SAM, and this requires admin permissions.

For each test user, store refresh token in vault:

* Login as the test user via gcloud. For password, see Test Horde spreadsheet in
  Broad Google Drive.
  ```shell
  gcloud auth login
  ```

* Look for test user's refresh token
  in `~/.config/gcloud/legacy_credentials/<TEST_USER_EMAIL/adc.json`
  , and line `refresh_token`.

* Write the refresh token to Broad Vault
  ```shell
  docker run -it --rm -v ${HOME}/.vault-token:/root/.vault-token 
  broadinstitute/dsde-toolbox:consul-0.20.0 vault write secret/dsde/terra/cli-test/test-users/<TEST_USER_EMAIL> refresh_token=<REFRESH_TOKEN>
  ```

* Create GHA secret. Update workflows' `Render config` jobs (
  id: `render_config`) to read secrets.

### Automated tests

All unit and integration tests are run nightly via GitHub action
against `broad-dev`. On test completion, a Slack notification is sent to the
Broad `#platform-foundation-alerts` channel. If you kick off a full test run
manually, it will not send a notification.

Running the tests locally on your machine and via GitHub actions uses the same
set of test users. While the nightly CLI tests should not leak resources (e.g.
workspaces, SAM groups), this often happens when debugging something locally.
There is a GitHub action specifically for cleaning up these leaked resources. It
can be triggered manually from the GitHub repo UI. There is an option to run the
cleanup in `dry-run` mode, which should show whether there are any leaked
resources without actually deleting them.

Some tests may start failing once the number of leaked resources gets too high.
Usually, this is a `terra workspace list` test that does not page through more
than ~30 workspaces. We could fix this test to be more resilient, but it's been
a useful reminder to kick off the cleanup GitHub action, so we haven't done that
yet. If you see unexpected failures around listing workspaces, try kicking off
the cleanup action and re-running.

The workflow can be found
at [cleanup-test-user-workspaces.yml](https://github.com/DataBiosphere/terra-cli/blob/main/.github/workflows/cleanup-test-user-workspaces.yml)

#### Test config per deployment

By default, tests run against Broad deployment. To run against a different
deployment:

* Create a new file
  under [Test config](https://github.com/DataBiosphere/terra-cli/tree/main/src/test/resources/testconfigs)
* Create a new `render-config.sh` which renders config for your deployment. Put
  the configs in a new directory under `rendered`, eg `rendered/<mydeployment>`.
  The name of this directory must match the name of the testConfig in the next
  step.
* Run tests with `-PtestConfig=<testconfigfilenamewithout.json>`

For example, consider the project that external resources are created in. The
Broad deployment uses a project in Broad GCP org; Verily deployment uses a
project in Verily GCP org.

### Test cromwell.config generation in notebook

* Open a terminal in Jupyter notebook, source build terra and set it up with a
  workspace
* Generate the config
  ```shell
  terra cromwell generate-config
  ```
  Optionally add `--dir=xxx` to specify the directory

### Troubleshooting

* If you see the
  error `Connecting to Docker daemon failed. Check that Docker is installed and running.`,
  check if Docker is running with correct permissions
  ```shell
  sudo chmod 666 /var/run/docker.sock
  ```

### Debugging tips

* ssh into Docker container where you can run `terra`:
    * Add to your
      test: `TestCommand.runCommand("app", "execute", "sleep 10000000000");`
    * Run test
    * After `sleep` executes, go to Docker Desktop and click on `cli` icon. Then
      you'll have a shell in the Docker container.

* Skip creating workspace, since it's slow:

* Change [`TestUser.chooseTestUserWithSpendAccess()`](https://github.
  com/DataBiosphere/terra-cli/blob/main/src/test/java/harness/baseclasses/SingleWorkspaceUnit.java#L18)
  to `TestUser.chooseTestUserWithOwnerAccess()`. Without this, you might get
  different users on different runs, and they won't have access to each other's
  workspaces.

* Comment out [deleting workspace](https://github.
  com/DataBiosphere/terra-cli/blob/main/src/test/java/harness/baseclasses/SingleWorkspaceUnit.java#L37-L49)

* Replace [creating workspace](https://github.
  com/DataBiosphere/terra-cli/blob/main/src/test/java/harness/baseclasses/SingleWorkspaceUnit.java#L33)
  with `userFacingId = <id-from-earlier-workspace>;`

-----

## Docker

The `docker/` directory contains files required to build the Docker image. All
files in the `scripts/` subdirectory are copied to the image, into a
subdirectory that is on the `$PATH`, and made executable.

Merging a PR and installing should take care of all this Docker image stuff for
you, so these notes are mostly useful for debugging/development when you need to
make an image available outside of that normal process.

* The `tools/local-dev.sh` and `install.sh` scripts pull the default image.
* The `tools/publish-release.sh` script builds and publishes a new image. It
  also updates the image path that the CLI uses to point to this newly published
  image.

### Pull an existing image

The gcr.io/terra-cli-dev registry is public readable, so anyone should be able
to pull images.

To use a specific Docker image from GCR:

1. Pull the image with that tag.
   ```shell
   docker pull gcr.io/terra-cli-dev/terra-cli/v0.0:b5fdce0
   ```
2. Update the image id that the CLI uses.
   ```shell
   terra config set image --image=gcr.io/terra-cli-dev/terra-cli/v0.0:b5fdce0
   ```

### Build a new image

For any change in the `docker/` directory to take effect:

1. Build a new image. This uses a short Git hash for the current commit as the
   tag. See the script comments for more options.
   ```shell
   > ./tools/build-docker.sh
   
   Generating an image tag from the Git commit hash
   Building the image
   [...Docker output...]
   Successfully built 6558c3bcb316
   Successfully tagged terra-cli/local:92d6e09
   terra-cli/local:92d6e09 successfully built
   ```
2. Update the image id that the CLI uses. (See output of previous command for
   image name and tag.)
   ```shell
   terra config set image --image=terra-cli/local:b5fdce0
   ```

### Publish a new image

To publish a new image to GCR:

1. Build the image (see above).
2. Render the CI credentials from Vault, in order to upload to GCR.
   ```shell
   ./tools/render-config.sh
   ```
3. Push it to GCR. (See output of build command for local image tag.) See the
   script comments for more options.
   ```shell
   > ./tools/publish-docker.sh 92d6e09 "terra-cli/test" 92d6e09
      
   Logging in to docker using the CI service account key file
   Login Succeeded
   Tagging the local docker image with the name to use in GCR
   Logging into to gcloud and configuring docker with the CI service account
   Activated service account credentials for: [dev-ci-sa@broad-dsde-dev.iam.gserviceaccount.com]
   Pushing the image to GCR
   [...Docker push output...]
   92d6e09: digest: sha256:f419d97735749573baff95247d0918d174cb683089c9b1370e7c99817b9b6d67 size: 2211
   Restoring the current gcloud user
   Updated property [core/account].
   gcr.io/terra-cli-dev/terra-cli/test:92d6e09 successfully pushed to GCR
   ```
4. Pull the image from GCR (see above). This is so that the name and tag on your
   local image matches what it will look like for someone who did not build the
   image.

### Update the default image

It's best to do this as part of a release, but if it's necessary to update the
default image manually:

1. Publish the image (see above).
2. Update the `DockerAppsRunner.defaultImageId` method in the Java code to
   return a hard-coded string.

-----

## Code

### Code structure

Below is an outline of the package structure. More details are included in the
subsections below.

```
bio.terra.cli      
    apps            # external supported tools
    businessobject  # internal state classes and business logic 
    cloud           # cloud-specific implementations
    command         # command definitions
    exception       # exception classes that map to an exit code
    serialization   # serialization format classes for command input/output and writing to disk
    service         # helper/wrapper classes for talking to Terra and cloud services
    utils           # uncategorized
```

### Supported tools

The `apps` package contains (external) tools that the CLI supports. Currently,
these tools can be called from the top-level, so it looks the same as it would
if you called it on your local terminal, only with a `terra` prefix. For
example:

```shell
terra gsutil ls
terra bq version
terra nextflow run hello
```

The list of supported tools that can be called is specified in an enum in
the `terra app list` class.

#### Add a new supported tool

To add a new supported tool:

1. Install the app in the `docker/Dockerfile`
2. Build the new image (see instructions in section above).
3. Test that the installation works by calling the app through
   the `terra app execute` command.
   (e.g. `terra app execute dsub --version`). This command just runs the Docker
   container and executes the command, without requiring any new Java code.
   This `terra app execute` command is intended for debugging only; this won't
   be how users call the tool.
4. Add a new command class in
   the `src/main/java/bio/terra/cli/command/app/passthrough` package. Copy/paste
   an existing class in that same package as a starting point.
5. Add it to the list of tools shown by `terra app list` by adding the new
   command class to the list of sub-commands in the `@Command` annotation of
   the `Main.class`. This means you can invoke the command by prefixing it with
   terra (e.g. `terra dsub -version`).
6. When you run e.g. `terra dsub -version`, the CLI:
    * [Docker mode only] Launches a Docker container
    * [Docker mode only] Runs the `terra_init.sh` script in the `docker/scripts`
      directory, which sets the workspace project
    * Runs the `dsub` command
7. Environment variables that are always
   set: `GOOGLE_SERVICE_ACCOUNT_EMAIL`, `GOOGLE_CLOUD_PROJECT`, `TERRA_<resource_name>`  
   If you need additional environment variables, you can pass them
   into `CommandRunner.runToolCommand()`.
8. You can mount directories on the host machine to the Docker container by
   populating a second `Map` and passing it to the
   same `DockerAppsRunner. runToolCommand` method. The current working directory
   is always mounted to the Docker container.
9. Publish the new Docker image and update the default image that the CLI uses
   to the new version (see instructions in section above).

### Business logic

The `businessobjects` package contains objects that represent the internal
state (e.g. `Config`, `Server`, `User`,
`Workspace`). Since the CLI Java code exits after each command, the `Context`
class persists the state on disk in the context directory `$HOME/.terra`.

### Commands

The `command` package contains the hierarchy of the commands as they appear to
the user. The directory structure matches the command hierarchy. The only
exceptions to this are the pass-through app commands (e.g. `terra gsutil`),
which are at the top level of the hierarchy, but in the `app/passthrough`
subdirectory.

`Main` is the top-level command and child commands are defined in class
annotations. Most of the top-level commands (e.g. `auth`, `server`, `workspace`)
are strictly for grouping; the command itself doesn't do anything.

### Serialization

There are 4 types of objects.

* Internal state and business logic
    * `businessobject` package
    * Maybe a part of the state (e.g. `Workspace`, `User`) or just contain
      business logic (e.g. `WorkspaceUser`)

* Serialization format for writing to disk (`.terra/context.json`)
    * `serialization.persisted` package
    * Prefixed with "PD" (e.g. `PDWorkspace`, `PDUser`)

* Serialization format for command input/ouput (json format)
    * `serialization.userfacing` package
    * Prefixed with "UF" (e.g. `UFWorkspace`, `UFUser`)

* Create/update parameters
    * `serialization.userfacing.inputs` package
    * Most of these parameter classes are not directly being used for
      user-facing input. They are put in a sub-package
      under `serialization.userfacing` because we might want to expose them to
      users in the future. e.g. By passing in a json file instead of specifying
      lots of options, as we do now for bucket lifecycle rules.

### Terra and cloud services

The `service` package contains classes that communicate with Terra and cloud
services. They contain retries and other error-handling that is specific to each
service. This functionality is not CLI-specific and could be moved into the
service's client library or a helper client library, in the future.

### Servers

The `src/main/java/resources/servers/` directory contains the server
specification files. Each file specifies a Terra environment, or set of
connected Terra services, and maps to an instance of the
`ServerSpecification` class.

The `ServerSpecification` class closely follows the Test Runner class of the
same name. There's no need to keep these classes exactly in sync, but that may
be a good place to consult when expanding the class here.

To add a new server specification, create a new file in this directory and add
the file name to the `all-servers.json`
file.

### Workspace & Resource IDs

In WSM db, `workspace` table has 2 ID columns: `workspace_id`
and `user_facing_id`. For simplicity, user only sees `user_facing_id`; for
example in `terra workspace describe.`

`uuid` does appear in `context.json` (and `.terra/logs/terra.log`). We
need `uuid` because WSM APIs take `uuid`, not `userFacingId`.

|                | What is this                                | Name in codebase | What CLI user sees          |
|----------------|---------------------------------------------|------------------|-----------------------------|
| workspace_id   | UUID; db primary key                        | `uuid`           | N/A, user doesn't see this. |
| user_facing_id | A human-settable, mutable, ID. Also unique. | `userFacingId`   | `ID`                        |

Similarly `resource` table has 2 ID columns: `resource_id`
and a mutable but unique `name`. For simplicity, user only sees `name`; for
example in `terra resource describe.`

### Adding a new resource type

See https://github.com/DataBiosphere/terra-cli/pull/276

-----

## Command style guide

Below are guidelines for adding or modifying commands. The goal is to have a
consistent presentation across commands. These are not hard rules, but should
apply to most commands. Always choose better usability over following a rule
here.

### Options instead of parameters

Use options instead of parameters, wherever possible.
e.g. `terra workspace add-user --email=user@gmail.com --role=READER`
instead of `terra workspace add-user user@gmail.com READER`.

This makes it easier to maintain backwards compatibility when adding new
arguments. It also makes it easier to read commands with multiple arguments,
without having to remember the order.

The exception to this rule is the `config set` command, which takes one
parameter instead of an option.

All option names should start with two dashes. e.g. `--email`

### Always specify a description

Specify a description for all commands and options. Write it like a sentence:
end with a period and capitalize the first letter of the first word.
e.g. `Add a user or group to the workspace.`, `Group name.`

Use a verb for the first word of a command or command group description.
e.g. `Manage spend profiles.`

### Alphabetize command lists

Alphabetize commands by their name (not their Java class name, though that is
usually identical) when specifying a list of sub-commands. picocli does not do
this automatically.

### User readable exception messages

`UserActionableException`s are expected in the course of normal use. Their
messages should be readable to the user.
`SystemException`s and any other exceptions are not expected in the course of
normal use. They indicate a bug or error that should be reported, so there's no
need to make these messages readable to the user.

### Singular command group names

Use singular command group names instead of plural. e.g. `terra resource`
instead of `terra resources`.

### Keep cloud specific code separated

Always prefer to keep cloud specific functions separate from each other and from
cloud-agnostic functions, but containing them in separate classes and/or
packages.

-----

## Auth overview

`terra` commands (`terra workspace`) run as user or pet SA, depending on where
they run. Tool
commands (`terra bq`) [always run as pet SA](https://github.com/DataBiosphere/terra-cli/search?q=getPetSACredentials)
, for two reasons:

* `terra` cli should not have access to *all* of a user's cloud resources --
  just the ones they use with `terra`.
* Some organizations prohibit their employees from granting
  `https://www.googleapis.com/auth/cloud-platform` scope to arbitrary tools.
  So `terra` cli OAuth does *not* grant
  `https://www.googleapis.com/auth/cloud-platform` scope. So user account cannot
  call GCP APIs. Instead, we use a pet SA access token which does
  have `https://www.googleapis.com/auth/cloud-platform` scope. (SAM stores pet
  SA keys, so SAM is able to generate an access token with that scope.)

Some tests use test user refresh
tokens ([example](https://github.com/DataBiosphere/terra-cli/blob/1f6e18eb7922cbc6c1ea6e7e80048ae79a8e3892/src/test/java/harness/TestUser.java#L120))
. These refresh tokens are stored in vault. Most places uses service account
keys to bypass manual OAuth in tests. However, our employer prohibits the use of
service account keys for security reasons, so we use refresh tokens instead.
Note that
[refresh tokens never expire](https://developers.google.com/identity/protocols/oauth2#expiration)
.

<table>
<tr>
<td></td>
<td>

Regular command (eg `terra workspace`)
</td>
<td>Tool command (Docker and local process mode)</td>
</tr>
<tr>
<td>Local computer</td>
<td valign="top">

**User Terra OAuth**

`terra auth login` CLI goes through oauth flow and saves credentials
in `.terra/StoredCredential`. No `gcloud` involved. Requests to services such as
WorkspaceManager use an access token obtained from `.terra/StoredCredential`.
</td>
<td>

**Pet user credentials or ADC, depending on tool**

Most tools accept both (user credentials or ADC). Even though we don't need ADC
for most
tools, [we currently require ADC for all tools](https://github.com/DataBiosphere/terra-cli/blob/79a938e56f2d95111aeec54175b6d38d9e5deb79/src/main/java/bio/terra/cli/app/DockerCommandRunner.java#L98)
.

Normally ADC is used with SA. When running Terra on a local computer, you are
using ADC with your user credentials. This is unusual. (And as mentioned
earlier, this is because we currently require ADC for all tools.) Please ignore
this warning:
`WARNING: Your application has authenticated using end user credentials from Google Cloud SDK. We recommend that most server applications use service accounts instead`
.

[Nextflow only works with ADC.](https://www.nextflow.io/docs/latest/google.html#credentials)
User is asked to run `gcloud auth application-default login`, which
writes  `.config/gcloud/application_default_credentials.json`.

In Docker
mode, [we mount `.config/gcloud`](https://github.com/DataBiosphere/terra-cli/blob/8adf7cdaaa1f74f9407c10cccc8f7c0c4623eb6b/src/main/java/bio/terra/cli/app/DockerCommandRunner.java#L80)
.

</td>
</tr>
<tr>
<td>GCP notebook</td>
<td>

**Pet ADC**

[Notebook startup script runs `terra auth login --mode=APP_DEFAULT_CREDENTIALS`.](https://github.com/DataBiosphere/terra-workspace-manager/blob/main/service/src/main/java/bio/terra/workspace/service/resource/controlled/cloud/gcp/ainotebook/post-startup.sh#L71)
So notebook `terra` commands will use ADC, which is automatically configured for
GCE VMs, rather than OAuth (which involves interacting with a browser).
</td>
<td valign="top">

**Pet ADC**

ADC is automatically configured for GCE VMs.
</td>
</tr>
<tr>
<td>Unit test</td>
<td valign="top">

**Test user Terra OAuth**

At the beginning of each test, before running terra
CLI, [test user is logged in](https://github.com/DataBiosphere/terra-cli/blob/8adf7cdaaa1f74f9407c10cccc8f7c0c4623eb6b/src/test/java/harness/baseclasses/SingleWorkspaceUnit.java#L30)
.

`TestUser.login()` [writes `.terra/StoredCredential`](https://github.com/DataBiosphere/terra-cli/blob/8adf7cdaaa1f74f9407c10cccc8f7c0c4623eb6b/src/test/java/harness/TestUser.java#L76)
,
using [domain-wide delegation](https://developers.google.com/admin-sdk/directory/v1/guides/delegation)
to avoid browser flow.
</td>
<td>

**Pet**

Auth is complicated because we are not using Pet SA key.

*Access token*

TestCommand.runCommand(): Set `IS_TEST` system property.

DockerCommand: If `IS_TEST` system property is set and there's a user and
workspace, set `CLOUDSDK_AUTH_ACCESS_TOKEN` to pet SA access token in container.
(Tests run in docker mode by default, so don't need this in
LocalProcessCommandrunner.)

*bq*

Here's what makes `cloudsdk/component_build/wrapper_scripts/bq.py` happy:

- Set `CLOUDSDK_AUTH_ACCESS_TOKEN` to access token
- Populate `.config/gcloud/legacy_credentials/default/adc.json`.

In general for gcloud, you only need `--access_token_file`
/`CLOUDSDK_AUTH_ACCESS_TOKEN`, not `adc.json`.
However, [implementation of `CLOUDSDK_AUTH_ACCESS_TOKEN`](https://cloud.google.com/sdk/docs/release-notes#cloud_sdk_2)
did not include `bq.py`, so we also need `adc.json`.

*gsutil*

There are 3 versions of `gsutil`:

1. Standalone `gsutil`. This is the oldest, and predates `gcloud`.
2. `gsutil` as part of gcloud
3. `gcloud alpha storage`. This is the
   newest. [Faster than 2.](https://stackoverflow.com/collectives/google-cloud/articles/68475140/faster-cloud-storage-transfers-using-the-gcloud-command-line)

For 2, we write the `~/.config/gcloud/legacy_credentials/default/.boto`
that  `google-cloud-sdk/bin/bootstrapping/gsutil.py` expects.

For 3, we set `CLOUDSDK_AUTH_ACCESS_TOKEN`.

*docker configuration*

[Mount `.config/gcloud`.](https://github.com/DataBiosphere/terra-cli/blob/8adf7cdaaa1f74f9407c10cccc8f7c0c4623eb6b/src/main/java/bio/terra/cli/app/DockerCommandRunner.java#L80)
</td>
</tr>
<tr>
<td>Integration test</td>
<td>

**Test user Terra OAuth**

At the beginning of each test, before running terra
CLI, [test user is logged in](https://github.com/DataBiosphere/terra-cli/blob/8adf7cdaaa1f74f9407c10cccc8f7c0c4623eb6b/src/test/java/harness/baseclasses/SingleWorkspaceUnit.java#L30)
.

[`TestUser.login()` writes `.terra/StoredCredential`](https://github.com/DataBiosphere/terra-cli/blob/8adf7cdaaa1f74f9407c10cccc8f7c0c4623eb6b/src/test/java/harness/TestUser.java#L76)
,
using [domain-wide delegation](https://developers.google.com/admin-sdk/directory/v1/guides/delegation)
to avoid browser flow.

</td>
<td valign="top">

**Pet ADC**

*Nextflow*

Populate `.config/gcloud/application_default_credentials.json`.

</td>
</tr>
</table>
