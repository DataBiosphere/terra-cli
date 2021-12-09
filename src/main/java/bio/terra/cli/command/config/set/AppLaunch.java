package bio.terra.cli.command.config.set;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set app-launch" command. */
@Command(name = "app-launch", description = "Configure the ways apps are launched.")
public class AppLaunch extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(AppLaunch.class);

  @CommandLine.Parameters(index = "0", description = "App launch mode: ${COMPLETION-CANDIDATES}.")
  private Config.CommandRunnerOption mode;

  /** Updates the command runner option property of the global context. */
  @Override
  protected void execute() {
    logger.debug("terra config set app-launch");
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
