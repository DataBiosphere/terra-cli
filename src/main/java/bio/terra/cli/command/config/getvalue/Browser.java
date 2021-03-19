package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra config get-value browser" command. */
@Command(
    name = "browser",
    description = "Check whether a browser is launched automatically during the login process.")
public class Browser implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();

    System.out.println(
        "Browser launch mode for login is " + globalContext.browserLaunchOption + ".");

    return 0;
  }
}
