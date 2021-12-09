package bio.terra.cli.command.config.set;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set browser" command. */
@Command(
    name = "browser",
    description = "Configure whether a browser is launched automatically during the login process.")
public class Browser extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Browser.class);

  @CommandLine.Parameters(
      index = "0",
      description = "Browser launch mode: ${COMPLETION-CANDIDATES}.")
  private Config.BrowserLaunchOption mode;

  /** Updates the browser launch option property of the global context. */
  @Override
  protected void execute() {
    logger.debug("terra config set browser");
    Config config = Context.getConfig();
    Config.BrowserLaunchOption prevBrowserLaunchOption = config.getBrowserLaunchOption();
    config.setBrowserLaunchOption(mode);

    OUT.println(
        "Browser launch mode for login is "
            + config.getBrowserLaunchOption()
            + " ("
            + (config.getBrowserLaunchOption() == prevBrowserLaunchOption ? "UNCHANGED" : "CHANGED")
            + ").");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
