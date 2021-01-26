package bio.terra.cli.command.auth;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.model.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth revoke" command. */
@Command(name = "revoke", description = "Revoke credentials from an account.")
public class Revoke implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    new AuthenticationManager(globalContext).logoutTerraUser();
    System.out.println("Logout successful.");
    return 0;
  }
}
