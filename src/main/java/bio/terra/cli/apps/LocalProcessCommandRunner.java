package bio.terra.cli.apps;

import bio.terra.cli.apps.utils.LocalProcessLauncher;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class runs client-side tools in a local process. */
public class LocalProcessCommandRunner extends CommandRunner {
  private static final Logger logger = LoggerFactory.getLogger(LocalProcessCommandRunner.class);

  private LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();

  public LocalProcessCommandRunner(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    super(globalContext, workspaceContext);
  }

  /**
   * Run a tool command in a local process.
   *
   * <p>Some setup/cleanup commands for gcloud authentication and configuration will be run
   * before/after the given command.
   *
   * @param command the command and arguments to execute
   * @param envVars a mapping of environment variable names to values
   */
  protected void runToolCommandImpl(List<String> command, Map<String, String> envVars) {
    List<String> bashCommands = new ArrayList<>();
    bashCommands.add("gcloud config set project " + workspaceContext.getGoogleProject());
    bashCommands.add(
        "gcloud auth activate-service-account --key-file=$GOOGLE_APPLICATION_CREDENTIALS");
    bashCommands.add(buildFullCommand(command));

    List<String> processCommand = new ArrayList<>();
    processCommand.add("bash");
    processCommand.add("-c");
    processCommand.add(String.join("; ", bashCommands));

    // set the path to the pet SA key file
    envVars.put(
        "GOOGLE_APPLICATION_CREDENTIALS",
        GlobalContext.getPetSaKeyFile(globalContext.requireCurrentTerraUser(), workspaceContext)
            .toString());

    // launch the child process
    localProcessLauncher.launchProcess(processCommand, envVars);

    // stream the output to stdout/err
    localProcessLauncher.streamOutputForProcess();

    // block until the child process exits
    int exitCode = localProcessLauncher.waitForTerminate();
    logger.debug("local process exit code: {}", exitCode);
  }
}
