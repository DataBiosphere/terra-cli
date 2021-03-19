package bio.terra.cli.command.config.set;

import bio.terra.cli.auth.AuthenticationManager.BrowserLaunchOption;
import bio.terra.cli.command.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set browser" command. */
@Command(
    name = "browser",
    description = "Configure whether a browser is launched automatically during the login process.")
public class Browser extends BaseCommand<Browser.ReturnValue> {

  @CommandLine.Parameters(
      index = "0",
      description = "Browser launch mode: ${COMPLETION-CANDIDATES}")
  private BrowserLaunchOption mode;

  @Override
  public ReturnValue execute() {
    globalContext.updateBrowserLaunchFlag(mode);

    return new ReturnValue(mode);
  }

  /**
   * The return value for this command is just the current value of the browser option in the global
   * context.
   */
  public static class ReturnValue extends BaseCommand.BaseReturnValue {
    public BrowserLaunchOption browser;

    public ReturnValue(BrowserLaunchOption browser) {
      this.browser = browser;
    }

    @Override
    public void printText() {
      output.println("browser launch for login = " + browser);
    }
  }
}
