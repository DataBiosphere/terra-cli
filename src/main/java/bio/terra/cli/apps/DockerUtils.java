package bio.terra.cli.apps;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.utils.Printer;
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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** This class provides utility methods for running Docker containers. */
public class DockerUtils {
  private final DockerClient dockerClient;

  public DockerUtils() {
    this.dockerClient = DockerUtils.buildDockerClient();
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
    }
  }

  /**
   * Start a Docker container and run the given command. Return the container id.
   *
   * @param imageId the id of the docker image to use for the container
   * @param command the full string command to execute in a bash shell (bash -c ..cmd..)
   * @param workingDir the directory where the commmand will be executed
   * @param envVars a mapping of environment variable names to values
   * @param bindMounts a mapping of container mount point to the local directory being mounted
   * @return id of the container that started
   * @throws SystemException if the local directory does not exist or is not a directory
   */
  public String startDockerContainer(
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
      File localDirectory = bindMount.getValue().toFile();
      if (!localDirectory.exists() || !localDirectory.isDirectory()) {
        throw new SystemException(
            "Bind mount does not specify a local directory: " + localDirectory.getAbsolutePath());
      }
      bindMountsObj.add(
          new Bind(localDirectory.getAbsolutePath(), new Volume(bindMount.getKey().toString())));
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
    CreateContainerResponse container = createContainerCmd.exec();
    dockerClient.startContainerCmd(container.getId()).exec();
    return container.getId();
  }

  /**
   * Block until the Docker container exits, then return its status code.
   *
   * @param containerId id of the container
   */
  public Integer waitForDockerContainerToExit(String containerId) {
    WaitContainerResultCallback waitContainerResultCallback = new WaitContainerResultCallback();
    WaitContainerResultCallback exec =
        dockerClient.waitContainerCmd(containerId).exec(waitContainerResultCallback);
    return exec.awaitStatusCode();
  }

  /**
   * Delete the Docker container.
   *
   * @param containerId id of the container
   */
  public void deleteDockerContainer(String containerId) {
    dockerClient.removeContainerCmd(containerId).exec();
  }

  /**
   * Read the Docker container logs and write them to standard out.
   *
   * @param containerId id of the container
   */
  public void outputLogsForDockerContainer(String containerId) {
    dockerClient
        .logContainerCmd(containerId)
        .withStdOut(true)
        .withStdErr(true)
        .withFollowStream(true)
        .withTailAll()
        .exec(new LogContainerTestCallback());
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
      String logStr = new String(frame.getPayload(), StandardCharsets.UTF_8);
      PrintWriter err = Printer.getErr();
      err.print(logStr);
      err.flush();

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
}
