package bio.terra.cli.command.config.set;

import bio.terra.cli.auth.AuthenticationManager.BrowserLaunchOption;
import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set browser" command. */
@Command(
    name = "browser",
    description = "Configure whether a browser is launched automatically during the login process.")
public class Browser implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description = "Browser launch mode: ${COMPLETION-CANDIDATES}")
  private BrowserLaunchOption mode;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();

    BrowserLaunchOption prevBrowserLaunchOption = globalContext.browserLaunchOption;
    globalContext.updateBrowserLaunchFlag(mode);

    System.out.println(
        "Browser launch mode for login is "
            + globalContext.browserLaunchOption
            + " ("
            + (globalContext.browserLaunchOption == prevBrowserLaunchOption
                ? "UNCHANGED"
                : "CHANGED")
            + ").");

    return 0;
  }
}
