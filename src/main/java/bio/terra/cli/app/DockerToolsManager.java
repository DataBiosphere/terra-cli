package bio.terra.cli.app;

import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.TerraUser;
import bio.terra.cli.model.WorkspaceContext;
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
public class DockerToolsManager {
  private static final Logger logger = LoggerFactory.getLogger(DockerToolsManager.class);

  private final GlobalContext globalContext;
  private final WorkspaceContext workspaceContext;
  private DockerClient dockerClient;

  // This is where the pet key files will be mounted on the Docker container.
  public static final String PET_KEYS_MOUNT_POINT = "/usr/local/etc/terra_cli";

  public DockerToolsManager(GlobalContext globalContext, WorkspaceContext workspaceContext) {
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

  /** Run a command inside the Docker container for external tools. */
  public String runToolCommand(String command) {
    return runToolCommand(command, null, new HashMap<>(), new HashMap<>());
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
  public String runToolCommand(
      String command,
      String workingDir,
      Map<String, String> envVars,
      Map<String, File> bindMounts) {
    // check that the current workspace is defined
    workspaceContext.requireCurrentWorkspace();

    buildDockerClient();

    // create and start the docker container. run the terra_init script first, then the given
    // command
    String containerId =
        startDockerContainerWithTerraInit(command, workingDir, envVars, bindMounts);

    // block until the container exits
    Integer statusCode = waitForDockerContainerToExit(containerId);
    logger.info("docker run status code: {}", statusCode);

    // read the container logs, which contains the command output
    String logs = getLogsForDockerContainer(containerId);

    // delete the container
    deleteDockerContainer(containerId);

    return logs;
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
        "PET_KEY_FILE",
        PET_KEYS_MOUNT_POINT + "/" + currentUser.getPetKeyFile(googleProjectId).getName());
    terraInitEnvVars.put("GOOGLE_PROJECT_ID", googleProjectId);
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

  /** Block until the Docker container exits, then return its status code. */
  private Integer waitForDockerContainerToExit(String containerId) {
    WaitContainerResultCallback waitContainerResultCallback = new WaitContainerResultCallback();
    WaitContainerResultCallback exec =
        dockerClient
            .waitContainerCmd(containerId)
            .<WaitContainerResultCallback>exec(waitContainerResultCallback);
    return exec.awaitStatusCode();
  }

  /** Read the Docker container logs into a string. TODO: How to handle very long output? */
  private String getLogsForDockerContainer(String containerId) {
    try {
      return dockerClient
          .logContainerCmd(containerId)
          .withStdOut(true)
          .withStdErr(true)
          .exec(new DockerToolsManager.LogContainerTestCallback())
          .awaitCompletion()
          .toString();
    } catch (InterruptedException intEx) {
      logger.error("Error reading logs for Docker container.", intEx);
      return "<ERROR READING CONTAINER LOGS>";
    }
  }

  /** Delete the Docker container. */
  private void deleteDockerContainer(String containerId) {
    dockerClient.removeContainerCmd(containerId).exec();
  }

  /** Helper class for reading Docker container logs into a string. */
  private static class LogContainerTestCallback extends ResultCallback.Adapter<Frame> {

    protected final StringBuffer log = new StringBuffer();
    List<Frame> collectedFrames = new ArrayList<>();
    boolean collectFrames = false;

    public LogContainerTestCallback() {
      this(false);
    }

    public LogContainerTestCallback(boolean collectFrames) {
      this.collectFrames = collectFrames;
    }

    @Override
    public void onNext(Frame frame) {
      if (collectFrames) collectedFrames.add(frame);
      log.append(new String(frame.getPayload(), Charset.forName("UTF-8")));
    }

    @Override
    public String toString() {
      return log.toString();
    }

    public List<Frame> getCollectedFrames() {
      return collectedFrames;
    }
  }
}
