package bio.terra.cli.apps;

import static bio.terra.cli.service.WorkspaceManager.getAiNotebookInstanceName;
import static bio.terra.cli.service.WorkspaceManager.getBigQueryDatasetPath;
import static bio.terra.cli.service.WorkspaceManager.getGcsBucketUrl;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.workspace.model.ResourceDescription;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Sub-classes of CommandRunner define different ways to run app/tool commands. */
public abstract class CommandRunner {
  private static final Logger logger = LoggerFactory.getLogger(CommandRunner.class);

  protected final GlobalContext globalContext;
  protected final WorkspaceContext workspaceContext;

  public CommandRunner(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    this.globalContext = globalContext;
    this.workspaceContext = workspaceContext;
  }

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

    // check that the current workspace is defined
    workspaceContext.requireCurrentWorkspace();

    // add Terra global and workspace context information as environment variables
    Map<String, String> terraEnvVars = buildMapOfTerraReferences();
    terraEnvVars.put(
        "GOOGLE_APPLICATION_CREDENTIALS",
        GlobalContext.getPetSaKeyFile(globalContext.requireCurrentTerraUser(), workspaceContext)
            .toString());
    terraEnvVars.put("GOOGLE_CLOUD_PROJECT", workspaceContext.getGoogleProject());
    for (Map.Entry<String, String> workspaceReferenceEnvVar : terraEnvVars.entrySet()) {
      if (envVars.get(workspaceReferenceEnvVar.getKey()) != null) {
        throw new SystemException(
            "Workspace reference cannot overwrite an environment variable used by the tool command: "
                + workspaceReferenceEnvVar.getKey());
      }
    }
    envVars.putAll(terraEnvVars);

    // call the sub-class implementation of running a tool command
    runToolCommandImpl(command, envVars);
  }

  /** This method defines how to execute the command, and must be implemented by each sub-class. */
  protected abstract void runToolCommandImpl(List<String> command, Map<String, String> envVars);

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
    workspaceContext
        .listResources()
        .forEach(
            resource ->
                terraReferences.put(
                    "TERRA_" + resource.getMetadata().getName(),
                    resolveResourceForCommandEnvVar(resource)));

    return terraReferences;
  }

  /**
   * Helper method for resolving a workspace resource into a cloud id, in order to pass it as an
   * environment variable to a tool command.
   *
   * @param resource workspace resource object
   * @return cloud id to set the environment variable to
   */
  private String resolveResourceForCommandEnvVar(ResourceDescription resource) {
    switch (resource.getMetadata().getResourceType()) {
      case GCS_BUCKET:
        return getGcsBucketUrl(resource);
      case BIG_QUERY_DATASET:
        return getBigQueryDatasetPath(resource);
      case AI_NOTEBOOK:
        return getAiNotebookInstanceName(resource);
      default:
        throw new UnsupportedOperationException(
            "Resource type not supported: " + resource.getMetadata().getResourceType());
    }
  }
}
