package bio.terra.cli.command.auth;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.TerraUser;
import bio.terra.cli.model.WorkspaceContext;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth status" command. */
@Command(name = "status", description = "Print details about the currently authorized account.")
public class Status implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).populateCurrentTerraUser();
    Optional<TerraUser> currentTerraUserOpt = globalContext.getCurrentTerraUser();

    // check if current user is defined
    if (!currentTerraUserOpt.isPresent()) {
      System.out.println("No current Terra user defined.");
      return 0;
    }
    TerraUser currentTerraUser = currentTerraUserOpt.get();
    System.out.println("Current Terra user: " + currentTerraUser.terraUserName);

    // check if the current user needs to re-authenticate (i.e. is logged out)
    System.out.println("LOGGED " + (currentTerraUser.requiresReauthentication() ? "OUT" : "IN"));

    return 0;
  }
}
