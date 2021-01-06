package bio.terra.cli.command.auth;

import bio.terra.cli.auth.AuthManager;
import bio.terra.cli.auth.TerraUser;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth status" command. */
@Command(name = "status", description = "Print details about the currently authorized account.")
public class Status implements Callable<Integer> {

  @Override
  public Integer call() {
    AuthManager authManager = AuthManager.buildAuthManagerFromGlobalContext();
    authManager.populateCurrentTerraUser();
    TerraUser currentTerraUser = authManager.getCurrentTerraUser();

    // check if current user is defined
    if (currentTerraUser == null) {
      System.out.println("There is no current Terra user defined.");
      return 0;
    }

    // check if the current user needs to re-authenticate (i.e. is logged out)
    if (currentTerraUser.requiresReauthentication()) {
      System.out.println(
          "The current Terra user (" + currentTerraUser.getTerraUserName() + ") is logged out.");
    } else {
      System.out.println(
          "The current Terra user (" + currentTerraUser.getTerraUserName() + ") is logged in.");
    }

    return 0;
  }
}
