package bio.terra.cli.app;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.PassthroughException;
import bio.terra.cli.exception.SystemException;
import com.google.common.annotations.VisibleForTesting;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sub-classes of CommandRunner define different ways to run app/tool commands (e.g. in a Docker
 * container, in a local child process). This class handles things that all of these options will
 * need, including building a map of environment variables to set before running the command. These
 * environment variables specify resolved workspace references and other context information.
 */
public abstract class CommandRunner {
  // Only tests set this.
  @VisibleForTesting public static final String IS_TEST = "IS_TEST";
  private static final Logger logger = LoggerFactory.getLogger(CommandRunner.class);

  /**
   * Utility method for concatenating a command and its arguments.
   *
   * @param command the command and arguments (e.g. {gsutil, ls, gs://my-bucket})
   */
  public static String buildFullCommand(List<String> command) {
    String fullCommand = "";
    if (command != null && command.size() > 0) {
      final String argSeparator = " ";
      fullCommand += argSeparator + String.join(argSeparator, command);
    }
    return fullCommand;
  }

  // Note: `foo-bar` and `foo_bar` will have the same env variable. PF-1907 will fix this.
  public static String convertToEnvironmentVariable(String string) {
    return "TERRA_" + string.replace("-", "_");
  }

  /**
   * If this is a test and there is a user and workspace, returns pet SA access token. Else, returns
   * empty.
   */
  public static Optional<String> getTestPetSaAccessToken() {
    if (System.getProperty(IS_TEST) != null
        && Context.getUser().isPresent()
        && Context.getWorkspace().isPresent()) {
      return Optional.of(Context.getUser().get().getPetSaAccessToken().getTokenValue());
    }
    return Optional.empty();
  }

  /**
   * Run a tool command. Passes global and workspace context information as environment variables:
   * pet SA email, workspace GCP project, resolved workspace resources.
   *
   * @param command the command and arguments to execute
   */
  public void runToolCommand(List<String> command) {
    runToolCommand(command, new HashMap<>());
  }

  /**
   * Run a tool command. Passes global and workspace context information as environment variables:
   * pet SA email, workspace GCP project, resolved workspace resources. Allows adding environment
   * variables beyond this, as long as the names don't conflict.
   *
   * @param command the command and arguments to execute
   * @param envVars a mapping of environment variable names to values
   * @throws SystemException if a Terra environment variable overlaps or conflicts with one passed
   *     into this method
   * @throws PassthroughException if the command returns a non-zero exit code
   */
  public void runToolCommand(List<String> command, Map<String, String> envVars) {
    for (String commandToken : command) {
      logger.debug("tokenized command string: {}", commandToken);
    }

    // add Terra global and workspace context information as environment variables
    Map<String, String> terraEnvVars = buildMapOfTerraReferences();

    // Keep in sync with notebook instance environment variables:
    // https://github.com/DataBiosphere/terra-workspace-manager/blob/42a96e3efe78908d2969d1ac826cd84a11a16714/service/src/main/java/bio/terra/workspace/service/resource/controlled/cloud/gcp/ainotebook/post-startup.sh#L165

    terraEnvVars.put("TERRA_USER_EMAIL", Context.requireUser().getEmail());
    terraEnvVars.put("GOOGLE_SERVICE_ACCOUNT_EMAIL", Context.requireUser().getPetSaEmail());
    terraEnvVars.put("GOOGLE_CLOUD_PROJECT", Context.requireWorkspace().getGoogleProjectId());

    for (Map.Entry<String, String> workspaceReferenceEnvVar : terraEnvVars.entrySet()) {
      if (envVars.get(workspaceReferenceEnvVar.getKey()) != null) {
        throw new SystemException(
            "Workspace reference cannot overwrite an environment variable used by the tool command: "
                + workspaceReferenceEnvVar.getKey());
      }
    }
    envVars.putAll(terraEnvVars);

    // call the sub-class implementation of running a tool command
    int exitCode = runToolCommandImpl(wrapCommandInSetupCleanup(command), envVars);

    // if the command is not successful, then pass the exit code out to the CLI caller
    if (exitCode != 0) {
      throw new PassthroughException(exitCode);
    }
  }

  /**
   * This method modifies the command to include any setup/cleanup commands.
   *
   * @param command the command and arguments to execute
   * @return the full string of commands and arguments to execute
   */
  protected abstract String wrapCommandInSetupCleanup(List<String> command);

  /**
   * This method defines how to execute the command, and must be implemented by each sub-class.
   *
   * @param command the full string of command and arguments to execute
   * @param envVars a mapping of environment variable names to values
   * @return process exit code
   */
  protected abstract int runToolCommandImpl(String command, Map<String, String> envVars);

  /**
   * Build a map of Terra references to use in setting environment variables when running commands.
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
    Context.requireWorkspace()
        .listResourcesAndSync()
        .forEach(
            resource -> {
              String envVariable = convertToEnvironmentVariable(resource.getName());
              terraReferences.put(envVariable, resource.resolve());
            });

    return terraReferences;
  }
}
