package bio.terra.cli.apps;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.GlobalContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sub-classes of CommandRunner define different ways to run app/tool commands (e.g. in a Docker
 * container, in a local child process). This class handles things that all of these options will
 * need, including building a map of environment variables to set before running the command. These
 * environment variables specify resolved workspace references and other context information.
 */
public abstract class CommandRunner {
  private static final Logger logger = LoggerFactory.getLogger(CommandRunner.class);

  public CommandRunner() {}

  /**
   * Utility method for concatenating a command and its arguments.
   *
   * @param command the command and arguments (e.g. {gsutil, ls, gs://my-bucket})
   */
  protected static String buildFullCommand(List<String> command) {
    String fullCommand = "";
    if (command != null && command.size() > 0) {
      final String argSeparator = " ";
      fullCommand += argSeparator + String.join(argSeparator, command);
    }
    return fullCommand;
  }

  /**
   * Run a tool command. Passes global and workspace context information as environment variables:
   * pet SA key file, workspace GCP project, resolved workspace resources.
   *
   * @param command the command and arguments to execute
   */
  public void runToolCommand(List<String> command) {
    runToolCommand(command, new HashMap<>());
  }

  /**
   * Run a tool command. Passes global and workspace context information as environment variables:
   * pet SA key file, workspace GCP project, resolved workspace resources. Allows adding environment
   * variables beyond this, as long as the names don't conflict.
   *
   * @param command the command and arguments to execute
   * @param envVars a mapping of environment variable names to values
   * @throws SystemException if a Terra environment variable overlaps or conflicts with one passed
   *     into this method
   */
  public void runToolCommand(List<String> command, Map<String, String> envVars) {
    for (String commandToken : command) {
      logger.debug("tokenized command string: {}", commandToken);
    }

    // add Terra global and workspace context information as environment variables
    Map<String, String> terraEnvVars = buildMapOfTerraReferences();
    terraEnvVars.put("GOOGLE_APPLICATION_CREDENTIALS", "");
    terraEnvVars.put(
        "GOOGLE_CLOUD_PROJECT", GlobalContext.get().requireCurrentWorkspace().googleProjectId);
    for (Map.Entry<String, String> workspaceReferenceEnvVar : terraEnvVars.entrySet()) {
      if (envVars.get(workspaceReferenceEnvVar.getKey()) != null) {
        throw new SystemException(
            "Workspace reference cannot overwrite an environment variable used by the tool command: "
                + workspaceReferenceEnvVar.getKey());
      }
    }
    envVars.putAll(terraEnvVars);

    // call the sub-class implementation of running a tool command
    runToolCommandImpl(wrapCommandInSetupCleanup(command), envVars);
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
   */
  protected abstract void runToolCommandImpl(String command, Map<String, String> envVars);

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
    GlobalContext.get()
        .requireCurrentWorkspace()
        .getResources()
        .forEach(resource -> terraReferences.put("TERRA_" + resource.name, resource.resolve()));

    return terraReferences;
  }
}
