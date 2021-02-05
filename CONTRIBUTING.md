# terra-cli

1. [Logging](#logging)
2. [Code structure](#code-structure)
    * [Docker](#docker)
    * [Servers](#servers)
    * [Command structure](#command-structure)
    * [Model and manager classes](#model-and-manager-classes)
    * [Supported tools](#supported-tools)
    * [Utility methods](#utility-methods)

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
      app/
        supported/
      command/
      model/
      utils/
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

#### Model and manager classes
The `src/main/java/bio/terra/cli/model/` directory contains the objects that represent the current state.
Since the CLI Java code exits after each command, this state is persisted on disk by the `GlobalContext` and 
`WorkspaceContext` classes.

The `src/main/java/bio/terra/cli/app/` directory contains `*Manager` classes that manipulate the context objects.

#### Supported tools
The `src/main/java/bio/terra/cli/app/supported/` directory contains (external) tools that the CLI supports.
Currently these tools can be called from the top-level, so it looks the same as it would if you called it on your 
local terminal, only with a `terra` prefix. For example:
```
terra gsutil ls
terra bq version
terra nextflow run hello
```

Tools can follow existing command patterns, such as `terra app enable [app name]` and `terra app stop [app name]`, by
implementing the accompanying interface in `src/main/java/bio/terra/cli/apps/interfaces/` and updating the enum in the
same class.

Alternatively, tools can forgo this existing pattern altogether and just add new commands without implementing any of
these interfaces.

This section of the code is likely to evolve significantly.

#### Utility methods
The `src/main/java/bio/terra/cli/utils/` directory contains utility methods that are not strictly related to the CLI.
