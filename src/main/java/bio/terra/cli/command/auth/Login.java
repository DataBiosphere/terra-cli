package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthManager;
import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth login" command. */
@Command(
    name = "login",
    description = "Authorize the CLI to access Terra APIs and data with user credentials.")
public class Login implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    new AuthManager(globalContext).loginTerraUser();
    System.out.println(
        "Login successful. (" + globalContext.getCurrentTerraUser().terraUserName + ")");
    return 0;
  }
}
