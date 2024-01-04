package bio.terra.cli.app;

import bio.terra.cli.app.utils.AppDefaultCredentialUtils;
import bio.terra.cli.app.utils.DockerClientWrapper;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.ExternalCredentialsManagerService;
import bio.terra.externalcreds.model.SshKeyPair;
import bio.terra.externalcreds.model.SshKeyPairType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * This class runs client-side tools in a Docker container and manipulates the tools-related
 * properties of the global context object.
 */
public class DockerCommandRunner extends CommandRunner {
  private static final Logger logger = LoggerFactory.getLogger(DockerCommandRunner.class);
  // default $HOME directory on the container (this is where we expect to look for the global
  // context)
  private static final String CONTAINER_HOME_DIR = "/root";
  // mount point for the workspace directory
  private static final String CONTAINER_WORKING_DIR = "/usr/local/etc";
  // name of the ADC file mounted on the container
  private static final String APPLICATION_DEFAULT_CREDENTIALS_FILE_NAME =
      "application_default_credentials.json";
  private final DockerClientWrapper dockerClientWrapper = new DockerClientWrapper();

  /**
   * Get the global context directory on the container.
   *
   * <p>e.g. (host) $HOME/.terra/ -> (container) CONTAINER_HOME_DIR/.terra/
   *
   * @return absolute path to the global context directory on the container
   */
  private static Path getGlobalContextDirOnContainer() {
    Path globalContextDirName = Context.getContextDir().getFileName();
    return Path.of(CONTAINER_HOME_DIR).resolve(globalContextDirName);
  }

  /**
   * This method builds a command string that:
   *
   * <p>- calls the terra_init.sh script, which configures gcloud with the workspace project
   *
   * <p>- runs the given command
   *
   * @param command the command and arguments to execute
   * @return the full string of commands and arguments to execute
   */
  protected String wrapCommandInSetupCleanup(List<String> command) {
    // the terra_init script is already copied into the Docker image
    return "terra_init.sh && ssh_init.sh && " + buildFullCommand(command);
  }

  /**
   * Run a tool command inside a new Docker container.
   *
   * <p>The terra_init.sh script that was copied into the Docker image will be run before the given
   * command.
   *
   * @param command the full string of command and arguments to execute
   * @param envVars a mapping of environment variable names to values
   * @return process exit code
   */
  protected int runToolCommandImpl(String command, Map<String, String> envVars)
      throws PassthroughException {
    // mount the global context directory and the current working directory to the container
    //  e.g. global context dir (host) $HOME/.terra -> (container) CONTAINER_HOME_DIR/.terra
    //       current working dir (host) /Users/mm/workspace123 -> (container) CONTAINER_WORKING_DIR
    Map<Path, Path> bindMounts = new HashMap<>();
    bindMounts.put(getGlobalContextDirOnContainer(), Context.getContextDir());
    bindMounts.put(Path.of(CONTAINER_WORKING_DIR), Path.of(System.getProperty("user.dir")));

    // mount the gcloud config directory to the container
    // e.g. gcloud config dir (host) $HOME/.config/gcloud -> (container)
    // CONTAINER_HOME_DIR/.config/gcloud
    Path gcloudConfigDir = Path.of(System.getProperty("user.home"), ".config/gcloud");
    Path gcloudConfigDirOnContainer = Path.of(CONTAINER_HOME_DIR, ".config/gcloud");
    if (gcloudConfigDir.toFile().exists() && gcloudConfigDir.toFile().isDirectory()) {
      bindMounts.put(gcloudConfigDirOnContainer, gcloudConfigDir);
    }

    // mount the .ssh directory to the container
    // e.g. (host) ssh dir $HOME/.ssh -> (container) CONTAINER_HOME_DIR/.ssh
    Path sshDir = Path.of(System.getProperty("user.home"), ".ssh");
    Path sshDirOnContainer = Path.of(CONTAINER_HOME_DIR, ".ssh");
    if (sshDir.toFile().exists() && sshDir.toFile().isDirectory()) {
      bindMounts.put(sshDirOnContainer, sshDir);
    }
    ExternalCredentialsManagerService ecmService = ExternalCredentialsManagerService.fromContext();
    SshKeyPair sshKeyPair = null;
    try {
      sshKeyPair = ecmService.getSshKeyPair(SshKeyPairType.GITHUB);
    } catch (SystemException e) {
      if (e.getCause() instanceof HttpStatusCodeException) {
        logger.warn("No terra ssh key, cannot set up ssh key in the docker container");
      }
    }
    if (sshKeyPair != null) {
      logger.debug(sshKeyPair.getPrivateKey());
      envVars.put("SSH_PRIVATE_KEY", sshKeyPair.getPrivateKey());
    }

    // For unit tests, set CLOUDSDK_AUTH_ACCESS_TOKEN. This is how to programmatically authenticate
    // as test user, without SA key file
    // (https://cloud.google.com/sdk/docs/release-notes#cloud_sdk_2).
    if (getTestPetSaAccessToken().isPresent()) {
      envVars.put("CLOUDSDK_AUTH_ACCESS_TOKEN", getTestPetSaAccessToken().get());
    } else { // this is normal operation
      // check that the ADC match the user or their pet SA
      AppDefaultCredentialUtils.throwIfADCDontMatchContext();

      // if the ADC are set by a file, then make sure that file is mounted to the container and the
      // env var points to it if needed
      Optional<Path> adcCredentialsFile = AppDefaultCredentialUtils.getADCBackingFile();
      if (adcCredentialsFile.isPresent()
          && adcCredentialsFile.get().equals(AppDefaultCredentialUtils.getDefaultGcloudADCFile())) {
        logger.info(
            "ADC backing file is in the default location and is already mounted in the gcloud config directory");
      } else {
        logger.info("ADC set by metadata server.");
      }
    }

    // create and start the docker container
    dockerClientWrapper.startContainer(
        Context.getConfig().getDockerImageId(),
        command,
        CONTAINER_WORKING_DIR,
        envVars,
        bindMounts);

    // read the container logs, which contains the command output, and write them to stdout
    dockerClientWrapper.streamLogsForContainer();

    // block until the container exits
    Integer statusCode = dockerClientWrapper.waitForContainerToExit();
    logger.debug("docker run status code: {}", statusCode);

    // get the process exit code
    Long exitCode = dockerClientWrapper.getProcessExitCode();
    logger.debug("docker inspect exit code: {}", exitCode);

    // delete the container
    dockerClientWrapper.deleteContainer();

    return exitCode.intValue();
  }
}
