package bio.terra.cli.command.datarefs;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs check-access" command. */
@CommandLine.Command(
    name = "check-access",
    description = "Check if you have access to a data reference.")
public class CheckAccess implements Callable<Integer> {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the data reference, scoped to the workspace.")
  private String name;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    CloudResource dataReference =
        new WorkspaceManager(globalContext, workspaceContext).getDataReference(name);

    TerraUser currentTerraUser = globalContext.requireCurrentTerraUser();
    boolean userHasAccess = dataReference.checkAccessForUser(currentTerraUser, workspaceContext);
    boolean petSaHasAccess = dataReference.checkAccessForPetSa(currentTerraUser, workspaceContext);

    System.out.println(
        "User ("
            + currentTerraUser.terraUserEmail
            + ") DOES "
            + (userHasAccess ? "" : "NOT ")
            + "have access to this data reference.");
    System.out.println(
        "User's pet SA in their proxy group ("
            + currentTerraUser.terraProxyGroupEmail
            + ") DOES "
            + (petSaHasAccess ? "" : "NOT ")
            + "have access to this data reference.");

    return 0;
  }
}
