package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthManager;
import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth revoke" command. */
@Command(name = "revoke", description = "Revoke credentials from an account.")
public class Revoke implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    new AuthManager(globalContext).logoutTerraUser();
    System.out.println("Logout successful.");
    return 0;
  }
}
