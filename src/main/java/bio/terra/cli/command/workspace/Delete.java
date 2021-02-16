package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import java.util.UUID;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace delete" command. */
@Command(name = "delete", description = "Delete an existing workspace.")
public class Delete implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    AuthenticationManager authenticationManager =
        new AuthenticationManager(globalContext, workspaceContext);
    authenticationManager.loginTerraUser();
    UUID workspaceIdDeleted =
        new WorkspaceManager(globalContext, workspaceContext).deleteWorkspace();
    authenticationManager.deletePetSaCredentials(globalContext.requireCurrentTerraUser());

    System.out.println("Workspace successfully deleted: " + workspaceIdDeleted);
    return 0;
  }
}
