package bio.terra.cli.app;

import bio.terra.cli.app.utils.AppDefaultCredentialUtils;
import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.workspace.model.CloudPlatform;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class runs client-side tools in a local process. */
public class LocalProcessCommandRunner extends CommandRunner {
  private static final Logger logger = LoggerFactory.getLogger(LocalProcessCommandRunner.class);

  /**
   * This method builds a command string that:
   *
   * <p>- saves the current gcloud project configuration
   *
   * <p>- configures gcloud with the workspace project
   *
   * <p>- runs the given command
   *
   * <p>- restores the gcloud configuration to the original project
   *
   * @param command the command and arguments to execute
   * @return the full string of commands and arguments to execute
   */
  protected String wrapCommandInSetupCleanup(List<String> command) {
    List<String> bashCommands = new ArrayList<>();
    boolean isGcpWorkspace =
        CloudPlatform.GCP.equals(Context.requireWorkspace().getCloudPlatform());

    String cmdPre = "";
    String cmdPost = "";

    if (isGcpWorkspace) {
      cmdPre = String.format("""
        TERRA_GCLOUD_PROJECT='%s'
        """, Context.requireWorkspace().getRequiredGoogleProjectId()) +
        """
        TERRA_PREV_GCLOUD_PROJECT="$(gcloud config get-value project)"
        if [[ "${TERRA_PREV_GCLOUD_PROJECT}" != "${TERRA_GCLOUD_PROJECT}" ]]; then
          echo "Setting the gcloud project to the workspace project: ${TERRA_GCLOUD_PROJECT}"
          gcloud config set project "${TERRA_GCLOUD_PROJECT}"
        fi
        """;

      cmdPost = """
        if [[ -z "${TERRA_PREV_GCLOUD_PROJECT}" ]]; then
          echo "Restoring the original gcloud project configuration: (unset)"
          gcloud config unset project
        elif [[ "${TERRA_PREV_GCLOUD_PROJECT}" != "${TERRA_GCLOUD_PROJECT}" ]]; then
          echo "Restoring the original gcloud project configuration: ${TERRA_PREV_GCLOUD_PROJECT}"
          gcloud config set project "${TERRA_PREV_GCLOUD_PROJECT}"
        fi
      """;
    }

    bashCommands.add(cmdPre);
    bashCommands.add(buildFullCommand(command));
    bashCommands.add(cmdPost);

    return String.join("\n", bashCommands);
  }

  /**
   * Run a tool command in a local process.
   *
   * <p>Some setup/cleanup commands for gcloud authentication and configuration will be run
   * before/after the given command.
   *
   * @param command the full string of command and arguments to execute
   * @param envVars a mapping of environment variable names to values
   * @return process exit code
   */
  protected int runToolCommandImpl(String command, Map<String, String> envVars)
      throws PassthroughException {
    if ("true".equals(System.getProperty(CommandRunner.IS_TEST))) {
      // For unit tests, set CLOUDSDK_AUTH_ACCESS_TOKEN. This is how to programmatically
      // authenticate as test user, without SA key file
      // (https://cloud.google.com/sdk/docs/authorizing).
      envVars.put("CLOUDSDK_AUTH_ACCESS_TOKEN", getTestPetSaAccessToken().orElseThrow());
    } else {
      // Only enforce ADC if we're not a test. (`gcloud auth activate-service-account` requires a
      // key file, which we don't want for security reasons.)

      // check that the ADC match the user or their pet SA
      AppDefaultCredentialUtils.throwIfADCDontMatchContext();

      // if the ADC are set by a file, then make sure the env var points to it if needed
      Optional<Path> adcCredentialsFile = AppDefaultCredentialUtils.getADCBackingFile();
      if (adcCredentialsFile.isPresent()
          && adcCredentialsFile.get().equals(AppDefaultCredentialUtils.getDefaultGcloudADCFile())) {
        logger.info("ADC backing file is in the default location");
      } else {
        logger.info("ADC set by metadata server.");
      }
    }

    return runBashCommand(command, envVars);
  }

  public static int runBashCommand(String command, Map<String, String> envVars) {
    List<String> processCommand = new ArrayList<>();
    processCommand.add("bash");
    processCommand.add("-ce");
    processCommand.add(command);

    // launch the child process
    LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();
    localProcessLauncher.launchProcess(processCommand, envVars);

    // stream the output to stdout/err
    localProcessLauncher.streamOutputForProcess();

    // block until the child process exits
    int exitCode = localProcessLauncher.waitForTerminate();
    logger.debug("local process exit code: {}", exitCode);

    return exitCode;
  }
}
