package bio.terra.cli.command.config.set;

import bio.terra.cli.command.BaseCommand;
import bio.terra.cli.context.GlobalContext;
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
  private ReturnValue.BrowserLaunchOptions mode;

  @Override
  public ReturnValue execute() {
    globalContext.updateBrowserLaunchFlag(mode.getBrowserLaunchFlag());

    return new ReturnValue(mode);
  }

  /** The return value for this command is just the current value of the boolean flag. */
  public static class ReturnValue extends BaseCommand.BaseReturnValue {
    public BrowserLaunchOptions browser;

    public ReturnValue(BrowserLaunchOptions browser) {
      this.browser = browser;
    }

    @Override
    public void printText() {
      output.println("browser launch for login = " + browser);
    }

    /**
     * This enum provides a text translation for the boolean flag {@link
     * GlobalContext#launchBrowserAutomatically}. This enum is not used in the auth logic, it is
     * strictly for CLI command presentation, so it lives here instead of the auth package.
     */
    public enum BrowserLaunchOptions {
      manual(false),
      auto(true);

      private boolean browserLaunchFlag;

      BrowserLaunchOptions(boolean browserLaunchFlag) {
        this.browserLaunchFlag = browserLaunchFlag;
      }

      public boolean getBrowserLaunchFlag() {
        return browserLaunchFlag;
      }

      public static BrowserLaunchOptions fromFlag(boolean browserLaunchFlag) {
        return browserLaunchFlag ? auto : manual;
      }
    }
  }
}
