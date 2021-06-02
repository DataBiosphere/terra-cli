package bio.terra.cli.command.config.set;

import bio.terra.cli.Config;
import bio.terra.cli.Context;
import bio.terra.cli.command.shared.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set app-launch" command. */
@Command(name = "app-launch", description = "Configure the ways apps are launched.")
public class AppLaunch extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "App launch mode: ${COMPLETION-CANDIDATES}")
  private Config.CommandRunnerOption mode;

  /** Updates the command runner option property of the global context. */
  @Override
  protected void execute() {
    Config config = Context.getConfig();
    Config.CommandRunnerOption prevAppLaunchOption = config.getCommandRunnerOption();
    config.setCommandRunnerOption(mode);

    OUT.println(
        "App launch mode is "
            + config.getCommandRunnerOption()
            + " ("
            + (config.getCommandRunnerOption() == prevAppLaunchOption ? "UNCHANGED" : "CHANGED")
            + ").");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
