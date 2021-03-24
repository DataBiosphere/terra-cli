package bio.terra.cli.command.config.set;

import bio.terra.cli.auth.AuthenticationManager.BrowserLaunchOption;
import bio.terra.cli.command.helperclasses.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set browser" command. */
@Command(
    name = "browser",
    description = "Configure whether a browser is launched automatically during the login process.")
public class Browser extends BaseCommand {

  @CommandLine.Parameters(
      index = "0",
      description = "Browser launch mode: ${COMPLETION-CANDIDATES}")
  private BrowserLaunchOption mode;

  /** Return the updated browser launch option property of the global context. */
  @Override
  protected void execute() {
    globalContext.updateBrowserLaunchFlag(mode);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
