package bio.terra.cli.command.auth;

import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth set-browser" command. */
@Command(
    name = "set-browser",
    description = "Configure whether a browser is launched automatically during the login process.")
public class SetBrowser implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description = "Browser launch mode: ${COMPLETION-CANDIDATES}")
  private BrowserLaunchOptions mode;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();

    boolean prevBrowserLaunchFlag = globalContext.launchBrowserAutomatically;
    globalContext.updateBrowserLaunchFlag(mode.equals(BrowserLaunchOptions.auto));

    System.out.println(
        "Browser will be launched "
            + (globalContext.launchBrowserAutomatically ? "automatically" : "manually")
            + " ("
            + (globalContext.launchBrowserAutomatically == prevBrowserLaunchFlag
                ? "UNCHANGED"
                : "CHANGED")
            + ")");

    return 0;
  }

  /**
   * This enum provides a text translation for the boolean flag {@link
   * GlobalContext#launchBrowserAutomatically}. This enum is not used in the auth logic, it is
   * strictly for CLI command presentation, so it lives here instead of the auth package.
   */
  private enum BrowserLaunchOptions {
    manual,
    auto;
  }
}
