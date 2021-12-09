package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get app-launch" command. */
@Command(name = "app-launch", description = "Check the way apps are launched.")
public class AppLaunch extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(AppLaunch.class);

  @CommandLine.Mixin Format formatOption;

  /** Return the command runner option property of the global context. */
  @Override
  protected void execute() {
    logger.debug("terra config get app-launch");
    formatOption.printReturnValue(Context.getConfig().getCommandRunnerOption());
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
