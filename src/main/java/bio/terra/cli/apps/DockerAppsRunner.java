package bio.terra.cli.apps;

import bio.terra.cli.command.exception.InternalErrorException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class runs client-side tools and manipulates the tools-related properties of the global
 * context object.
 */
public class DockerAppsRunner {
  private static final Logger logger = LoggerFactory.getLogger(DockerAppsRunner.class);

  private final GlobalContext globalContext;
  private final WorkspaceContext workspaceContext;
  private DockerClient dockerClient;

  // default $HOME directory on the container (this is where we expect to look for the global
  // context)
  private static final String CONTAINER_HOME_DIR = "/root";
  // mount point for the workspace directory
  private static final String CONTAINER_WORKSPACE_DIR = "/usr/local/etc";

  public DockerAppsRunner(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    this.globalContext = globalContext;
    this.workspaceContext = workspaceContext;
    this.dockerClient = buildDockerClient();
  }

  // ====================================================
  // Docker images

  // This variable specifies the default Docker image id or tag that the CLI uses to run external
  // tools. Keep this property in-sync with the dockerImageName Gradle property
  private static final String DEFAULT_DOCKER_IMAGE_ID =
      "gcr.io/terra-cli-dev/terra-cli/v0.1:stable";

  /** Returns the default image id. */
  public static String defaultImageId() {
    return DEFAULT_DOCKER_IMAGE_ID;
  }

  /**
   * Update the Docker image property of the global context.
   *
   * @param imageId id or tag of the image
   * @return true if the Docker image property was updated, false otherwise
   */
  public boolean updateImageId(String imageId) {
    // check if image exists
    try {
      dockerClient.inspectImageCmd(imageId).exec();
    } catch (NotFoundException nfEx) {
      logger.error("Image not found: {}", imageId, nfEx);
      return false;
    }

    globalContext.updateDockerImageId(imageId);
    return true;
  }

  // ====================================================
  // Tool commands

  /**
   * Utility method for concatenating a command and its arguments.
   *
   * @param cmd command name (e.g. gsutil)
   * @param cmdArgs command arguments (e.g. ls, gs://my-bucket)
   */
  public static String buildFullCommand(String cmd, List<String> cmdArgs) {
    String fullCommand = cmd;
    if (cmdArgs != null && cmdArgs.size() > 0) {
      final String argSeparator = " ";
      fullCommand += argSeparator + String.join(argSeparator, cmdArgs);
    }
    return fullCommand;
  }

  /**
   * Run a command inside the Docker container for external tools.
   *
   * <p>This method substitutes any Terra references in the command and also adds the Terra
   * references as environment variables in the container.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   */
  public void runToolCommand(String command) {
    runToolCommand(command, new HashMap<>());
  }

  /**
   * Run a command inside the Docker container for external tools. Allows adding environment
   * variables beyond what the terra_init script requires. The environment variables expected by the
   * terra_init script will be added to those passed in.
   *
   * <p>This method substitutes any Terra references in the command and also adds the Terra
   * references as environment variables in the container.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param envVars a mapping of environment variable names to values
   * @throws RuntimeException if an environment variable or bind mount used by the terra_init script
   *     overlaps or conflicts with one passed into this method
   */
  public void runToolCommand(String command, Map<String, String> envVars) {
    // check that the current workspace is defined
    workspaceContext.requireCurrentWorkspace();

    // add the Terra references as environment variables in the container
    Map<String, String> terraReferences = buildMapOfTerraReferences();
    for (Map.Entry<String, String> workspaceReferenceEnvVar : terraReferences.entrySet()) {
      if (envVars.get(workspaceReferenceEnvVar.getKey()) != null) {
        throw new InternalErrorException(
            "Workspace reference cannot overwrite an environment variable used by the tool command: "
                + workspaceReferenceEnvVar.getKey());
      }
    }
    envVars.putAll(terraReferences);

    // mount the global context directory and the workspace directory to the container
    //  e.g. global context dir (host) $HOME/.terra -> (container) CONTAINER_HOME_DIR/.terra
    //       workspace context dir (host) /Users/mm/workspace123 -> (container) CONTAINER_HOME_DIR
    Map<Path, Path> bindMounts = new HashMap<>();
    bindMounts.put(getGlobalContextDirOnContainer(), GlobalContext.getGlobalContextDir());
    bindMounts.put(Path.of(CONTAINER_WORKSPACE_DIR), WorkspaceContext.getWorkspaceDir());

    // create and start the docker container. run the terra_init script first, then the given
    // command
    String containerId = startDockerContainerWithTerraInit(command, envVars, bindMounts);

    // read the container logs, which contains the command output, and write them to stdout
    outputLogsForDockerContainer(containerId);

    // block until the container exits
    Integer statusCode = waitForDockerContainerToExit(containerId);
    logger.debug("docker run status code: {}", statusCode);

    // delete the container
    deleteDockerContainer(containerId);
  }

  /**
   * Build a map of Terra references to use in parsing the CLI command string, and in setting
   * environment variables in the Docker container.
   *
   * <p>The list of references are TERRA_[...] where [...] is the name of a cloud resource. The
   * cloud resource can be controlled or external.
   *
   * <p>e.g. TERRA_MY_BUCKET -> gs://terra-wsm-test-9b7511ab-my-bucket
   *
   * @return a map of Terra references (name -> cloud id)
   */
  private Map<String, String> buildMapOfTerraReferences() {
    // build a map of reference string -> resolved value
    Map<String, String> terraReferences = new HashMap<>();
    workspaceContext
        .listCloudResources()
        .forEach(
            cloudResource ->
                terraReferences.put(
                    "TERRA_" + cloudResource.name.toUpperCase(), cloudResource.cloudId));

    return terraReferences;
  }

  /**
   * Add the environment variables and bind mounts expected by the terra_init script to those passed
   * into this method. Then create and start the container. Login is required before running the
   * terra_init script because it tries to read the pet key file.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @throws RuntimeException if an environment variable or bind mount used by the terra_init script
   *     overlaps or conflicts with one passed into this method
   */
  private String startDockerContainerWithTerraInit(
      String command, Map<String, String> envVars, Map<Path, Path> bindMounts) {
    // check that there is a current user, because the terra_init script will try to read the pet
    // key file
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call the terra_init script that was copied into the Docker image, before running the given
    // command
    String fullCommand = "terra_init.sh && " + command;

    // the terra_init script relies on environment variables to pass in global and workspace
    // context information
    Map<String, String> terraInitEnvVars = new HashMap<>();
    terraInitEnvVars.put(
        "GOOGLE_APPLICATION_CREDENTIALS",
        getPetSaKeyFileOnContainer(currentUser, workspaceContext).toString());
    terraInitEnvVars.put("GOOGLE_CLOUD_PROJECT", workspaceContext.getGoogleProject());

    for (Map.Entry<String, String> terraInitEnvVar : terraInitEnvVars.entrySet()) {
      if (envVars.get(terraInitEnvVar.getKey()) != null) {
        throw new InternalErrorException(
            "Tool command cannot overwrite an environment variable used by the terra_init script: "
                + terraInitEnvVar.getKey());
      }
    }
    envVars.putAll(terraInitEnvVars);

    return startDockerContainer(
        fullCommand, getWorkingDirOnContainer().toString(), envVars, bindMounts);
  }

  // ====================================================
  // Docker containers

  /** Build the Docker client object with standard options. */
  private static DockerClient buildDockerClient() {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    DockerHttpClient httpClient =
        new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
    return DockerClientImpl.getInstance(config, httpClient);
  }

  /**
   * Start a Docker container and run the given command. Return the container id.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param workingDir the directory where the commmand will be executed
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @throws RuntimeException if the local directory does not exist or is not a directory
   */
  private String startDockerContainer(
      String command, String workingDir, Map<String, String> envVars, Map<Path, Path> bindMounts) {
    // flatten the environment variables from a map, into a list of key=val strings
    List<String> envVarsStr = new ArrayList<>();
    for (Map.Entry<String, String> envVar : envVars.entrySet()) {
      envVarsStr.add(envVar.getKey() + "=" + envVar.getValue());
    }

    // create Bind objects for each specified mount
    List<Bind> bindMountsObj = new ArrayList<>();
    for (Map.Entry<Path, Path> bindMount : bindMounts.entrySet()) {
      File localDirectory = bindMount.getValue().toFile();
      if (!localDirectory.exists() || !localDirectory.isDirectory()) {
        throw new InternalErrorException(
            "Bind mount does not specify a local directory: " + localDirectory.getAbsolutePath());
      }
      bindMountsObj.add(
          new Bind(localDirectory.getAbsolutePath(), new Volume(bindMount.getKey().toString())));
    }

    // create the container and start it
    CreateContainerCmd createContainerCmd =
        dockerClient
            .createContainerCmd(globalContext.dockerImageId)
            .withCmd("bash", "-c", command)
            .withEnv(envVarsStr)
            .withHostConfig(HostConfig.newHostConfig().withBinds(bindMountsObj))
            .withAttachStdout(true)
            .withAttachStderr(true);
    if (workingDir != null) {
      createContainerCmd.withWorkingDir(workingDir);
    }
    CreateContainerResponse container = createContainerCmd.exec();
    dockerClient.startContainerCmd(container.getId()).exec();
    return container.getId();
  }

  /**
   * Block until the Docker container exits, then return its status code.
   *
   * @param containerId id of the container
   */
  private Integer waitForDockerContainerToExit(String containerId) {
    WaitContainerResultCallback waitContainerResultCallback = new WaitContainerResultCallback();
    WaitContainerResultCallback exec =
        dockerClient
            .waitContainerCmd(containerId)
            .<WaitContainerResultCallback>exec(waitContainerResultCallback);
    return exec.awaitStatusCode();
  }

  /**
   * Delete the Docker container.
   *
   * @param containerId id of the container
   */
  private void deleteDockerContainer(String containerId) {
    dockerClient.removeContainerCmd(containerId).exec();
  }

  /**
   * Read the Docker container logs and write them to standard out.
   *
   * @param containerId id of the container
   */
  private void outputLogsForDockerContainer(String containerId) {
    dockerClient
        .logContainerCmd(containerId)
        .withStdOut(true)
        .withStdErr(true)
        .withFollowStream(true)
        .withTailAll()
        .exec(new DockerAppsRunner.LogContainerTestCallback());
  }

  /** Helper class for reading Docker container logs into a string. */
  private static class LogContainerTestCallback extends ResultCallback.Adapter<Frame> {

    protected final StringBuffer log = new StringBuffer();
    List<Frame> framesList = new ArrayList<>();

    // these two boolean flags are useful for debugging
    // buildSingleStringOutput = concatenate the output into a single String (be careful of very
    // long outputs)
    boolean buildSingleStringOutput;
    // buildFramesList = keep a list of all the output lines (frames) as they come back
    boolean buildFramesList;

    public LogContainerTestCallback() {
      this(false, false);
    }

    public LogContainerTestCallback(boolean buildSingleStringOutput, boolean buildFramesList) {
      this.buildSingleStringOutput = buildSingleStringOutput;
      this.buildFramesList = buildFramesList;
    }

    @Override
    public void onNext(Frame frame) {
      String logStr = new String(frame.getPayload(), Charset.forName("UTF-8"));

      // TODO: PF-423. Calling sysout.println here breaks the model of printing user-facing output
      // from the command classes only.
      // Revisit this once we have better model for centralizing output across all commands/rest of
      // the codebase.
      System.out.print(logStr); // write to stdout

      if (buildSingleStringOutput) {
        log.append(logStr);
      }
      if (buildFramesList) {
        framesList.add(frame);
      }
    }

    @Override
    public String toString() {
      return log.toString();
    }

    public List<Frame> getFramesList() {
      return framesList;
    }
  }

  // ====================================================
  // Directory and file names

  /**
   * Get the working directory on the container so that it matches the current working directory on
   * the host.
   *
   * <p>e.g. working dir on host = /Users/mm/workspace123/nextflow/rnaseq-nf
   *
   * <p>working dir on container = CONTAINER_WORKSPACE_DIR/nextflow/rnaseq-nf
   *
   * @return absolute path to the working directory on the container
   */
  private static Path getWorkingDirOnContainer() {
    // get the current working directory and the workspace directory on the host
    Path currentDir = Paths.get("").toAbsolutePath();
    Path workspaceDirOnHost = WorkspaceContext.getWorkspaceDir();

    // remove the workspace directory part of the current working directory
    Path relativePathToCurrentDir = workspaceDirOnHost.relativize(currentDir);

    // working directory on container = workspace dir on container + relative path to current dir
    return Path.of(CONTAINER_WORKSPACE_DIR).resolve(relativePathToCurrentDir);
  }

  /**
   * Get the global context directory on the container.
   *
   * <p>e.g. (host) $HOME/.terra/ -> (container) CONTAINER_HOME_DIR/.terra/
   *
   * @return absolute path to the global context directory on the container
   */
  private static Path getGlobalContextDirOnContainer() {
    Path globalContextDirName = GlobalContext.getGlobalContextDir().getFileName();
    return Path.of(CONTAINER_HOME_DIR).resolve(globalContextDirName);
  }

  /**
   * Get the pet SA key file for the given user and workspace on the container.
   *
   * <p>e.g. (host) $HOME/.terra/pet-keys/[user id]/[workspace id] -> (container)
   * CONTAINER_HOME_DIR/.terra/pet-keys/[user id]/[workspace id]
   *
   * @return absolute path to the pet SA key file for the given user and workspace on the container
   */
  private static Path getPetSaKeyFileOnContainer(
      TerraUser terraUser, WorkspaceContext workspaceContext) {
    // get the full path of the key file and global context directory on the host
    Path keyFileOnHost = GlobalContext.getPetSaKeyFile(terraUser, workspaceContext);
    Path globalContextDirOnHost = GlobalContext.getGlobalContextDir();

    // remove the global context directory part of the key file path
    // e.g. keyFileOnHost = $HOME/.terra/pet-keys/[user id]/[workspace id]
    //      globalContextDirOnHost = $HOME/.terra/
    //      relativePathToKeyFile = pet-keys/[user id]/[workspace id]
    Path relativePathToKeyFile = globalContextDirOnHost.relativize(keyFileOnHost);

    // key file path on container = global context dir on container + relative path to key file
    return getGlobalContextDirOnContainer().resolve(relativePathToKeyFile);
  }
}
