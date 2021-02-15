package bio.terra.cli.apps;

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

  // This is where the pet key files will be mounted on the Docker container.
  public static final String PET_KEYS_MOUNT_POINT = "/usr/local/etc/terra_cli";

  public DockerAppsRunner(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    this.globalContext = globalContext;
    this.workspaceContext = workspaceContext;
    this.dockerClient = null;
  }

  // This variable specifies the default Docker image id or tag that the CLI uses to run external
  // tools.
  // TODO: change this to a DockerHub or GCR path
  private static final String DEFAULT_DOCKER_IMAGE_ID = "terra/cli:v0.0";

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
    buildDockerClient();

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

  /**
   * Run a command inside the Docker container for external tools.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   */
  public void runToolCommand(String command) {
    runToolCommand(command, null, new HashMap<>(), new HashMap<>());
  }

  /**
   * Run a command inside the Docker container for external tools. Allows adding environment
   * variables and bind mounts beyond what the terra_init script requires. The environment variables
   * and bind mounts expected by the terra_init script will be added to those passed in.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param workingDir the directory where the commmand will be executed
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @throws RuntimeException if an environment variable or bind mount used by the terra_init script
   *     overlaps or conflicts with one passed into this method
   */
  public void runToolCommand(
      String command,
      String workingDir,
      Map<String, String> envVars,
      Map<String, File> bindMounts) {
    // check that the current workspace is defined
    workspaceContext.requireCurrentWorkspace();

    // substitute any Terra references in the command
    // also add the Terra references as environment variables in the container
    Map<String, String> terraReferences = buildMapOfTerraReferences();
    command = replaceTerraReferences(terraReferences, command);
    envVars.putAll(terraReferences);

    buildDockerClient();

    // create and start the docker container. run the terra_init script first, then the given
    // command
    String containerId =
        startDockerContainerWithTerraInit(command, workingDir, envVars, bindMounts);

    // read the container logs, which contains the command output, and write them to stdout
    outputLogsForDockerContainer(containerId);

    // block until the container exits
    Integer statusCode = waitForDockerContainerToExit(containerId);
    logger.info("docker run status code: {}", statusCode);

    // delete the container
    deleteDockerContainer(containerId);
  }

  /** Build the Docker client object with standard options. */
  private void buildDockerClient() {
    DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    DockerHttpClient httpClient =
        new ApacheDockerHttpClient.Builder()
            .dockerHost(config.getDockerHost())
            .sslConfig(config.getSSLConfig())
            .build();
    dockerClient = DockerClientImpl.getInstance(config, httpClient);
  }

  /**
   * Add the environment variables and bind mounts expected by the terra_init script to those passed
   * into this method. Then create and start the container. Login is required before running the
   * terra_init script because it tries to read the pet key file.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param workingDir the directory where the commmand will be executed
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @throws RuntimeException if an environment variable or bind mount used by the terra_init script
   *     overlaps or conflicts with one passed into this method
   */
  private String startDockerContainerWithTerraInit(
      String command,
      String workingDir,
      Map<String, String> envVars,
      Map<String, File> bindMounts) {
    // check that there is a current user, because the terra_init script will try to read the pet
    // key file
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call the terra_init script that was copied into the Docker image, before running the given
    // command
    String fullCommand = "terra_init.sh && " + command;

    // the terra_init script relies on environment variables to pass in global and workspace
    // context information
    Map<String, String> terraInitEnvVars = new HashMap<>();
    String googleProjectId = workspaceContext.getGoogleProject();
    terraInitEnvVars.put(
        "GOOGLE_APPLICATION_CREDENTIALS",
        PET_KEYS_MOUNT_POINT + "/" + currentUser.getPetKeyFile(googleProjectId).getName());
    terraInitEnvVars.put("TERRA_GOOGLE_PROJECT_ID", googleProjectId);
    for (Map.Entry<String, String> terraInitEnvVar : terraInitEnvVars.entrySet()) {
      if (envVars.get(terraInitEnvVar.getKey()) != null) {
        throw new RuntimeException(
            "Tool command cannot overwrite an environment variable used by the terra_init script: "
                + terraInitEnvVar.getKey());
      }
    }
    envVars.putAll(terraInitEnvVars);

    // the terra_init script requires that the pet SA key file is accessible to it
    Map<String, File> terraInitBindMounts = new HashMap<>();
    terraInitBindMounts.put(PET_KEYS_MOUNT_POINT, GlobalContext.resolvePetSaKeyDir().toFile());
    for (Map.Entry<String, File> terraInitBindMount : terraInitBindMounts.entrySet()) {
      if (bindMounts.get(terraInitBindMount.getKey()) != null) {
        throw new RuntimeException(
            "Tool command cannot bind mount to the same directory used by the terra_init script: "
                + terraInitBindMount.getKey());
      }
    }
    bindMounts.putAll(terraInitBindMounts);

    return startDockerContainer(fullCommand, workingDir, envVars, bindMounts);
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
      String command,
      String workingDir,
      Map<String, String> envVars,
      Map<String, File> bindMounts) {
    // flatten the environment variables from a map, into a list of key=val strings
    List<String> envVarsStr = new ArrayList<>();
    for (Map.Entry<String, String> envVar : envVars.entrySet()) {
      envVarsStr.add(envVar.getKey() + "=" + envVar.getValue());
    }

    // create Bind objects for each specified mount
    List<Bind> bindMountsObj = new ArrayList<>();
    for (Map.Entry<String, File> bindMount : bindMounts.entrySet()) {
      File localDirectory = bindMount.getValue();
      if (!localDirectory.exists() || !localDirectory.isDirectory()) {
        throw new RuntimeException(
            "Bind mount does not specify a local directory: "
                + bindMount.getValue().getAbsolutePath());
      }
      bindMountsObj.add(
          new Bind(bindMount.getValue().getAbsolutePath(), new Volume(bindMount.getKey())));
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
   * Build a map of Terra references to use in parsing the CLI command string, and in setting
   * environment variables in the Docker container.
   *
   * <p>The list of references are TERRA_[...] where [...] is the name of a cloud resource. The
   * cloud resource can be controlled or external.
   *
   * <p>e.g. TERRA_MY_BUCKET -> gs://terra-wsm-test-9b7511ab-my-bucket
   *
   * @return
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
   * Replace any Terra references in the command string with the resolved values. The references are
   * case insensitive.
   *
   * @param cmd the original command string
   * @return the modified command string
   */
  private String replaceTerraReferences(Map<String, String> terraReferences, String cmd) {
    // loop through the map entries
    String modifiedCmd = cmd;
    for (Map.Entry<String, String> terraReference : terraReferences.entrySet()) {
      // loop through the map entries, replacing each one in the command string (case insensitive)
      modifiedCmd =
          modifiedCmd.replaceAll(
              "(?i)\\{" + terraReference.getKey() + "}", terraReference.getValue());
    }
    return modifiedCmd;
  }
}
