package bio.terra.cli.command.workspace;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.WorkspaceManager;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace mount" command. */
@Command(name = "mount", description = "Mount an existing workspace to the current directory.")
public class Mount implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "workspace id")
  private String workspaceId;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext).requireCurrentTerraUser();
    new WorkspaceManager(globalContext, workspaceContext).mountWorkspace(workspaceId);
    System.out.println(
        "Workspace successfully mounted. (" + workspaceContext.getWorkspaceId() + ")");
    return 0;
  }
}
