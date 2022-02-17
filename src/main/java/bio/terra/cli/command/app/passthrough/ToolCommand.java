package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.app.utils.LocalProcessLauncher;
import bio.terra.cli.businessobject.Config.CommandRunnerOption;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public abstract class ToolCommand extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(ToolCommand.class);
  /** Return the name used to invoke this command in the shell. */
  public abstract String getExecutableName();

  /** A non-operational argument, such as version, to check command availability. */
  public String getVersionArgument() {
    return "version";
  }

  @CommandLine.Mixin protected WorkspaceOverride workspaceOption;

  @CommandLine.Unmatched protected final List<String> command = new ArrayList<>();

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    if (CommandRunnerOption.LOCAL_PROCESS == Context.getConfig().getCommandRunnerOption()
        && !toolIsInstalled()) {
      throw new UserActionableException(
          String.format(
              "Unable to launch %s. Please verify it is installed and included in the PATH.",
              getExecutableName()));
    }
    executeImpl();
  }

  /**
   * Implementation of the execution function. Expected to be overridden by nontrivial tool
   * commands.
   */
  protected void executeImpl() {
    workspaceOption.overrideIfSpecified();
    // no need for any special setup or teardown logic since command is already initialized when the
    // container starts
    command.add(0, getExecutableName());
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }

  /**
   * Launch a no-op process to determine if the tool is installed. Don't use the full CommandRunner,
   * because we don't want to set environment variables or clean up.
   */
  protected boolean toolIsInstalled() {
    List<String> noOpCommand = ImmutableList.of(getExecutableName(), getVersionArgument());
    // launch the No-op child process
    LocalProcessLauncher localProcessLauncher = new LocalProcessLauncher();
    try {
      localProcessLauncher.launchProcess(noOpCommand, new HashMap<>());
      // Output should not show to the screen
      // block until the child process exits
      int exitCode = localProcessLauncher.waitForTerminate();
      return 0 == exitCode;
    } catch (SystemException e) {
      logger.debug(
          "Failed to launch local process to check tool version for {}: ", getExecutableName(), e);
      // Likely the process failed to start, so we assume it isn't installed.
      return false;
    }
  }
}
