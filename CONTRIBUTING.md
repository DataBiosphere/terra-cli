# terra-cli

1. [Logging](#logging)
3. [Docker](#docker)
    * [Build a new image](#build-a-new-image)
    * [Pull an existing image](#pull-an-existing-image)
    * [Publish a new image](#publish-a-new-image)
    * [Update the default image](#update-the-default-image)
2. [Code structure](#code-structure)
    * [Servers](#servers)
    * [Command structure](#command-structure)
    * [Context classes](#context-classes)
    * [Supported tools](#supported-tools)
3. [Adding a new supported tool](#adding-a-new-supported-tool)

-----

### Logging
Logging is turned off by default. Modify the root level in the `src/main/resources/logback.xml` file to turn it on (e.g. `INFO`).

### Docker
The `docker/` directory contains files required to build the Docker image.
All files in the `scripts/` sub-directory are copied to the image, into a sub-directory that is on the `$PATH`, 
and made executable.

The `gradle.properties` file specifies the path to the default Docker image used by the CLI.

#### Build a new image
For any change in this directory to take effect:
1. Build a new image.
    ```
    > ./gradlew buildDockerImage
    [...Docker build output...]
    terra-cli/local:b5fdce0 successfully built
    
    BUILD SUCCESSFUL in 25s
    1 actionable task: 1 executed
    ```
2. Update the image id that the CLI uses. (See output of previous command for image name and tag.)
    ```
    > terra app set-image terra-cli/local:b5fdce0
    ```

#### Pull an existing image
The `tools/local-dev.sh` script pulls the default image already.

To use a specific Docker image from GCR:
1. Pull the image with that tag.
    ```
    > ./gradlew pullDockerImage -PdockerImageTag=b5fdce0
    Task :pullDockerImage
    b5fdce0: Pulling from terra-cli-dev/terra-cli/v0.0
    Digest: sha256:c77ee0b87a8972ec2a9f1b69387216a9f726f5503679edab37911a6322876dbe
    Status: Downloaded newer image for gcr.io/terra-cli-dev/terra-cli/v0.0:b5fdce0
    gcr.io/terra-cli-dev/terra-cli/v0.0:b5fdce0
    
    BUILD SUCCESSFUL in 3s
    1 actionable task: 1 executed
    ```
2. Update the image id that the CLI uses. (See output of previous command for image name and tag.)
    ```
    > terra app set-image gcr.io/terra-cli-dev/terra-cli/v0.0:b5fdce0
    ```

#### Publish a new image
To publish a new image to GCR:
1. Build the image (see above).
2. Push it to GCR.
    ```
    > ./gradlew publishDockerImage -PdockerLocalImageTag=b5fdce0
      
    Task :publishDockerImage
    Reading the CI service account key file from Vault
    Logging in to docker using this key file
    Login Succeeded
    Tagging the local docker image with the name to use in GCR
    Pushing the image to GCR
    The push refers to repository [gcr.io/terra-cli-dev/terra-cli/v0.0]
    [...Docker push output...]
    gcr.io/terra-cli-dev/terra-cli/v0.0:b5fdce0 successfully pushed to GCR
    
      BUILD SUCCESSFUL in 26s
      1 actionable task: 1 executed
    ```
3. Update the image id that the CLI uses. (See output of previous command for image name and tag.)
    ```
    > terra app set-image gcr.io/terra-cli-dev/terra-cli/v0.0:b5fdce0
    ```

#### Update the default image
To update the default image:
1. Build the image (see above).
2. Tag the image locally with `stable`. (See output of build command for local image name and tag.)
    ```
    docker tag terra-cli/local:b5fdce0 terra-cli/local:stable
    ```
3. Bump the version number in the image name in `gradle.properties`.
    ```
    dockerImageName=terra-cli/v0.1
    ```
4. Publish the new (`stable`-tagged) image to GCR (see above).
5. Update the `DockerAppsRunner.DEFAULT_DOCKER_IMAGE_ID` property in the Java code.


### Code structure
Below is an outline of the directory structure. Details about each are included in the sub-sections below.
```
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

### Add a new supported tool
To add a new supported tool:
   1. Install the app in the `docker/Dockerfile`
   2. Build the new image (see instructions in section above).
   3. Set the image that the CLI will use 
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
   9. Publish the new Docker image and update the default image that the CLI uses to the new version
   (see instructions above).
