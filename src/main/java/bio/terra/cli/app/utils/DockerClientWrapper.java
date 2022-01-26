package bio.terra.cli.app.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.UserIO;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.SELContext;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class provides utility methods for running Docker containers. */
public class DockerClientWrapper {
  private static final Logger logger = LoggerFactory.getLogger(DockerClientWrapper.class);

  private final DockerClient dockerClient;
  private String containerId;

  public DockerClientWrapper() {
    this.dockerClient = DockerClientWrapper.buildDockerClient();
  }

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
   * Check if the Docker image id exists on the local machine.
   *
   * @param imageId the id of the docker image to look for
   * @return true if the given image id exists
   */
  public boolean checkImageExists(String imageId) {
    try {
      dockerClient.inspectImageCmd(imageId).exec();
      return true;
    } catch (NotFoundException nfEx) {
      return false;
    } catch (RuntimeException rtEx) {
      throw wrapExceptionIfDockerConnectionFailed(rtEx);
    }
  }

  /**
   * Start a Docker container and run the given command.
   *
   * <p>Note this method cannot be called concurrently, because it updates the internal state of
   * this instance with the container id.
   *
   * @param imageId the id of the docker image to use for the container
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param workingDir the directory where the commmand will be executed
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @throws SystemException if the local directory does not exist or is not a directory
   */
  public void startContainer(
      String imageId,
      String command,
      String workingDir,
      Map<String, String> envVars,
      Map<Path, Path> bindMounts) {
    // flatten the environment variables from a map, into a list of key=val strings
    List<String> envVarsStr = new ArrayList<>();
    for (Map.Entry<String, String> envVar : envVars.entrySet()) {
      envVarsStr.add(envVar.getKey() + "=" + envVar.getValue());
    }

    // create Bind objects for each specified mount
    List<Bind> bindMountsObj = new ArrayList<>();
    for (Map.Entry<Path, Path> bindMount : bindMounts.entrySet()) {
      File localFileOrDirectory = bindMount.getValue().toFile();
      if (!localFileOrDirectory.exists()) {
        throw new SystemException(
            "Bind mount does not specify a local file or directory: "
                + localFileOrDirectory.getAbsolutePath());
      }
      bindMountsObj.add(
          new Bind(
              localFileOrDirectory.getAbsolutePath(),
              new Volume(bindMount.getKey().toString()),
              AccessMode.rw,
              SELContext.shared));
    }

    // create the container and start it
    CreateContainerCmd createContainerCmd =
        dockerClient
            .createContainerCmd(imageId)
            .withCmd("bash", "-c", command)
            .withEnv(envVarsStr)
            .withHostConfig(HostConfig.newHostConfig().withBinds(bindMountsObj))
            .withAttachStdout(true)
            .withAttachStderr(true);
    if (workingDir != null) {
      createContainerCmd.withWorkingDir(workingDir);
    }
    try {
      containerId = createContainerCmd.exec().getId();

      dockerClient.startContainerCmd(containerId).exec();

      logger.debug("container id: {}", containerId);
    } catch (RuntimeException rtEx) {
      throw wrapExceptionIfDockerConnectionFailed(rtEx);
    }
  }

  /** Block until the Docker container exits, then return its status code. */
  public Integer waitForContainerToExit() {
    WaitContainerResultCallback waitContainerResultCallback = new WaitContainerResultCallback();
    try {
      WaitContainerResultCallback exec =
          dockerClient.waitContainerCmd(containerId).exec(waitContainerResultCallback);
      return exec.awaitStatusCode();
    } catch (RuntimeException rtEx) {
      throw wrapExceptionIfDockerConnectionFailed(rtEx);
    }
  }

  /** Delete the Docker container. */
  public void deleteContainer() {
    try {
      dockerClient.removeContainerCmd(containerId).exec();
    } catch (RuntimeException rtEx) {
      throw wrapExceptionIfDockerConnectionFailed(rtEx);
    }
  }

  /** Inspect the container state and return the exit code. */
  public Long getProcessExitCode() {
    InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
    return container.getState().getExitCodeLong();
  }

  /** Read the Docker container logs and write them to standard out. */
  public void streamLogsForContainer() {
    try {
      dockerClient
          .logContainerCmd(containerId)
          .withStdOut(true)
          .withStdErr(true)
          .withFollowStream(true)
          .withTailAll()
          .exec(new LogContainerCommandCallback());
    } catch (RuntimeException rtEx) {
      throw wrapExceptionIfDockerConnectionFailed(rtEx);
    }
  }

  /** Helper class for reading Docker container logs into a string. */
  private static class LogContainerCommandCallback extends ResultCallback.Adapter<Frame> {

    protected final StringBuffer log = new StringBuffer();
    List<Frame> framesList = new ArrayList<>();

    // these two boolean flags are useful for debugging
    // buildSingleStringOutput = concatenate the output into a single String (be careful of very
    // long outputs)
    boolean buildSingleStringOutput;
    // buildFramesList = keep a list of all the output lines (frames) as they come back
    boolean buildFramesList;

    public LogContainerCommandCallback() {
      this(false, false);
    }

    public LogContainerCommandCallback(boolean buildSingleStringOutput, boolean buildFramesList) {
      this.buildSingleStringOutput = buildSingleStringOutput;
      this.buildFramesList = buildFramesList;
    }

    @Override
    public void onNext(Frame frame) {
      String logStr = new String(frame.getPayload(), StandardCharsets.UTF_8);
      PrintStream out = UserIO.getOut();
      out.print(logStr);
      out.flush();

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
   * Check if the given exception indicates that connecting to the Docker daemon failed. This
   * usually means that Docker is either not installed or not running.
   *
   * <p>- If this exception was caused by a Docker connection failure, then this method wraps the
   * given exception in a new RuntimeException with a more readable error message. Previously, it
   * was an obscure connection refused error.
   *
   * <p>- If this exception was NOT caused by a Docker connection failure, then this method returns
   * the given exception, unchanged.
   *
   * @param ex exception to check
   * @return a RuntimeException for the caller to re-throw
   */
  private RuntimeException wrapExceptionIfDockerConnectionFailed(RuntimeException ex) {
    boolean isDockerConnectionFailed =
        ex.getCause() != null
            && ex.getCause() instanceof IOException
            && ex.getCause().getMessage() != null
            && (ex.getCause().getMessage().toLowerCase().contains("connection refused")
                || ex.getCause().getMessage().contains("native connect() failed"));
    if (isDockerConnectionFailed) {
      return new UserActionableException(
          "Connecting to Docker daemon failed. Check that Docker is installed and running. "
              + "To run apps without Docker, use the LOCAL_PROCESS app launch mode (terra config set app-launch LOCAL_PROCESS).",
          ex);
    } else {
      return ex;
    }
  }
}
