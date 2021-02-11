# terra-cli

1. [Logging](#logging)
2. [Code structure](#code-structure)
    * [Docker](#docker)
    * [Servers](#servers)
    * [Command structure](#command-structure)
    * [Context classes](#context-classes)
    * [Supported tools](#supported-tools)
        * [Adding a new supported tool](#adding-a-new-supported-tool)

-----

### Logging
Logging is turned off by default. Modify the root level in the `src/main/resources/logback.xml` file to turn it on (e.g. `INFO`).

### Code structure
Below is an outline of the directory structure. Details about each are included in the sub-sections below.
```
docker/
  scripts/
src/main/
  java/
    bio/terra/cli/
      apps/
        interfaces/
      auth/
      command/
      context/
      service/
  resources/
      servers/
```

#### Docker
The `docker/` directory contains files required to build the Docker image.
All files in the `scripts/` sub-directory are copied to the image, into a sub-directory that is on the `$PATH`, 
and made executable.

From the `docker/` directory, run
```
docker build . --tag terra/cli:v0.0
```
Use the `terra app set-image` command to update the image used to launch supported applications.
This command accepts either the tag or the image id, which is output from the `docker build` command.

To update the default image tag, modify the `ToolsManager.DEFAULT_DOCKER_IMAGE_ID` property in the Java code.

#### Servers
The `src/main/java/resources/servers/` directory contains the server specification files.
Each file specifies a Terra environment, or set of connected Terra services, and maps to an instance of the 
`ServerSpecification` class.

The `ServerSpecification` class closely follows the Test Runner class of the same name.
There's no need to keep these classes exactly in sync, but that may be a good place to consult when expanding the 
class here.

To add a new server specification, create a new file in this directory and add the file name to the `all-servers.json` 
file.

#### Command structure
The `src/main/java/bio/terra/cli/command/` directory contains the hierarchy of the commands as they appear to the user.
With the exception of the `app/supported` sub-directory, the directory structure matches the command hierarchy.

`Main` is the top-level command and child commands are defined in class annotations.
Most of the top-level commands (e.g. `auth`, `server`, `workspace`) are strictly for grouping; the command itself 
doesn't do anything.
An empty class body with child commands defined in the annotation creates such a grouping command.

Supported tools are currently children of the top-level command, but are hidden from the usage help.

#### Context classes
The `src/main/java/bio/terra/cli/context/` directory contains the objects that represent the current state.
Since the CLI Java code exits after each command, this state is persisted on disk by the `GlobalContext` and 
`WorkspaceContext` classes.

Other packages (e.g. `src/main/java/bio/terra/cli/app/`, `auth/`, `server/` and `workspace/`) contain classes that 
manipulate the context objects. Classes are grouped into packages by general functional area.

#### Supported tools
The `src/main/java/bio/terra/cli/apps/` directory contains (external) tools that the CLI supports.
Currently these tools can be called from the top-level, so it looks the same as it would if you called it on your 
local terminal, only with a `terra` prefix. For example:
```
terra gsutil ls
terra bq version
terra nextflow run hello
```

The list of supported tools that can be called is specified in an enum in the `terra app list` class.

#### Adding a new supported tool
To add a new supported tool:
   1. Install the app in the `docker/Dockerfile`
   2. Build the new image with `source tools/local-dev.sh`
   3. Test that the install worked by calling the app through the `terra app execute` command.
   (e.g. `terra app execute dsub --version`). This command just runs the Docker container and 
   executes the command, without requiring any new Java code. This `terra app execute` command
   is intended for debugging only; this won't be how users call the tool.
   4. Add a new command class in the `src/main/java/bio/terra/cli/command/app/passthrough` package.
   Copy/paste an existing class in that same package as a starting point. Gsutil, Bq, and Gcloud 
   are the simplest.
   5. Add it to the list of tools shown by `terra app list` by adding the new command class to
   the list of sub-commands in the `@Command` annotation of the `Main.class`. This means you can
   invoke the command by prefixing it with terra (e.g. `terra dsub -version`).
   6. When you run e.g. `terra dsub -version`, the CLI:
      - Launches a Docker container
      - Runs the `terra_init.sh` script in the `docker/scripts` directory, which activates the user’s
      pet service account and sets the workspace project
      - Runs the dsub command
   7. You can pass environment variables through to the Docker container by populating a `Map` and
   passing it to the `DockerAppsRunner.runToolCommand` method. Two environment variables are always
   passed:
       - `GOOGLE_PROJECT_ID` = the workspace project id
       - `PET_KEY_FILE` = the pet service account key file
   8.  You can mount directories on the host machine to the Docker container by populating a second
   `Map` and passing it to the same `DockerAppsRunner.runToolCommand` method. The `nextflow` command
   has an example of this (see `bio.terra.cli.apps.Nextflow` class `run` method).