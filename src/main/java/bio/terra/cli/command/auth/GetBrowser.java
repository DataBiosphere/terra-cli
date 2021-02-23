package bio.terra.cli.command.auth;

import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth get-browser" command. */
@Command(
    name = "get-browser",
    description = "Check whether a browser is launched automatically during the login process.")
public class GetBrowser implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();

    System.out.println(
        "Browser will be launched "
            + (globalContext.launchBrowserAutomatically ? "automatically" : "manually")
            + ".");

    return 0;
  }
}
