package bio.terra.cli.command.auth;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.model.GlobalContext;
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
    new AuthenticationManager(globalContext).loginTerraUser();
    System.out.println(
        "Login successful. (" + globalContext.getCurrentTerraUser().get().terraUserName + ")");
    return 0;
  }
}
