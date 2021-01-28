package bio.terra.cli.app;

import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.TerraUser;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class runs applications and manipulates the tools-related properties of the global context
 * object.
 */
public class AppsManager {
  private static final Logger logger = LoggerFactory.getLogger(AppsManager.class);

  private final GlobalContext globalContext;
  private DockerClient dockerClient;

  // This is where the pet key files will be mounted on the Docker container.
  public static final String PET_KEYS_MOUNT_POINT = "/etc/terra_cli";

  public AppsManager(GlobalContext globalContext) {
    this.globalContext = globalContext;
    this.dockerClient = null;
  }

  // This variable specifies the default Docker image that the CLI uses to run external
  // applications/tools.
  // TODO: change this to a GCR path
  private static final String DEFAULT_DOCKER_IMAGE_ID = "5dd738808032";

  /** Returns the default image id. */
  public static String defaultImageId() {
    return DEFAULT_DOCKER_IMAGE_ID;
  }

  /** Run a command inside the Docker container for external applications/tools. */
  public String runAppCommand(String command) {
    return runAppCommand(command, new HashMap<>(), new HashMap<>());
  }

  /**
   * Run a command inside the Docker container for external applications/tools. Allows adding
   * environment variables and bind mounts beyond what the terra_init script requires. The
   * environment variables and bind mounts expected by the terra_init script will be added to those
   * passed in.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @throws RuntimeException if an environment variable or bind mount used by the terra_init script
   *     overlaps or conflicts with one passed into this method
   */
  public String runAppCommand(
      String command, Map<String, String> envVars, Map<String, File> bindMounts) {
    // create and start the docker container. run the terra_init script first, then the given
    // command
    buildDockerClient();
    String containerId = startDockerContainerWithTerraInit(command, envVars, bindMounts);

    // block until the container exits
    Integer statusCode = waitForDockerContainerToExit(containerId);
    logger.debug("docker run status code: {}", statusCode);

    // read the container logs, which contains the command output
    return getLogsForDockerContainer(containerId);
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
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @throws RuntimeException if an environment variable or bind mount used by the terra_init script
   *     overlaps or conflicts with one passed into this method
   */
  private String startDockerContainerWithTerraInit(
      String command, Map<String, String> envVars, Map<String, File> bindMounts) {
    // check that there is a current user, because the terra_init script will try to read the pet
    // key file
    Optional<TerraUser> currentUser = globalContext.getCurrentTerraUser();
    if (!currentUser.isPresent()) {
      throw new RuntimeException("Login required before running apps.");
    }

    // call the terra_init script that was copied into the Docker image, before running the given
    // command
    final String terraInitScript = "chmod a+x /usr/local/bin/terra_init.sh && terra_init.sh";
    String fullCommand = terraInitScript + " && " + command;

    // the terra_init script relies on environment variables to pass in (global and workspace)
    // context information
    Map<String, String> terraInitEnvVars = new HashMap<>();
    // TODO: get the google project id from the workspace context object
    String googleProjectId = "terra-cli-poc-1";
    terraInitEnvVars.put(
        "PET_KEY_FILE",
        PET_KEYS_MOUNT_POINT
            + "/"
            + globalContext.getCurrentTerraUser().get().getPetKeyFile(googleProjectId).getName());
    terraInitEnvVars.put("GOOGLE_PROJECT_ID", "terra-cli-poc-1");
    for (Map.Entry<String, String> terraInitEnvVar : terraInitEnvVars.entrySet()) {
      if (envVars.get(terraInitEnvVar.getKey()) != null) {
        throw new RuntimeException(
            "App command cannot overwrite an environment variable used by the terra_init script: "
                + terraInitEnvVar.getKey());
      }
    }
    envVars.putAll(terraInitEnvVars);

    // the terra_init script requires that the pet SA key file is accessible to it
    Map<String, File> terraInitBindMounts = new HashMap<>();
    terraInitBindMounts.put(PET_KEYS_MOUNT_POINT, GlobalContext.resolvePetSAKeyDir().toFile());
    for (Map.Entry<String, File> terraInitBindMount : terraInitBindMounts.entrySet()) {
      if (bindMounts.get(terraInitBindMount.getKey()) != null) {
        throw new RuntimeException(
            "App command cannot bind mount to the same directory used by the terra_init script: "
                + terraInitBindMount.getKey());
      }
    }
    bindMounts.putAll(terraInitBindMounts);

    return startDockerContainer(fullCommand, envVars, bindMounts);
  }

  /**
   * Start a Docker container and run the given command. Return the container id.
   *
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @throws RuntimeException if the local directory does not exist or is not a directory
   */
  private String startDockerContainer(
      String command, Map<String, String> envVars, Map<String, File> bindMounts) {
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
    CreateContainerResponse container =
        dockerClient
            .createContainerCmd(globalContext.dockerImageId)
            .withCmd("bash", "-c", command)
            .withEnv(envVarsStr)
            .withHostConfig(HostConfig.newHostConfig().withBinds(bindMountsObj))
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec();
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
          .exec(new AppsManager.LogContainerTestCallback())
          .awaitCompletion()
          .toString();
    } catch (InterruptedException intEx) {
      logger.error("Error reading logs for Docker container.", intEx);
      return "<ERROR READING CONTAINER LOGS>";
    }
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
