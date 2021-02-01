package bio.terra.cli.command.workspace;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.WorkspaceManager;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import java.util.UUID;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace delete" command. */
@Command(name = "delete", description = "Delete an existing workspace.")
public class Delete implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext).requireCurrentTerraUser();
    UUID workspaceIdDeleted =
        new WorkspaceManager(globalContext, workspaceContext).deleteWorkspace();
    System.out.println("Workspace successfully deleted. (" + workspaceIdDeleted + ")");
    return 0;
  }
}
