package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get browser" command. */
@Command(
    name = "browser",
    description = "Check whether a browser is launched automatically during the login process.")
public class Browser extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Browser.class);

  @CommandLine.Mixin Format formatOption;

  /** Return the browser launch option property of the global context. */
  @Override
  protected void execute() {
    logger.debug("terra config get browser");
    formatOption.printReturnValue(Context.getConfig().getBrowserLaunchOption());
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
