package bio.terra.cli.command.config.set;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.context.GlobalContext;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set app-launch" command. */
@Command(name = "app-launch", description = "Configure the ways apps are launched.")
public class AppLaunch extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "App launch mode: ${COMPLETION-CANDIDATES}")
  private GlobalContext.CommandRunners mode;

  /** Updates the command runner option property of the global context. */
  @Override
  protected void execute() {
    GlobalContext.CommandRunners prevAppLaunchOption = globalContext.commandRunnerOption;
    globalContext.updateCommandRunnerOption(mode);

    OUT.println(
        "App launch mode is "
            + globalContext.commandRunnerOption
            + " ("
            + (globalContext.commandRunnerOption == prevAppLaunchOption ? "UNCHANGED" : "CHANGED")
            + ").");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
