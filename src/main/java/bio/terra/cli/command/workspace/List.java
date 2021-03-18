package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.WorkspaceDescription;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace list" command. */
@Command(
    name = "list",
    description = "List all workspaces the current user can access.",
    showDefaultValues = true)
public class List implements Callable<Integer> {

  @CommandLine.Option(
      names = "--offset",
      required = false,
      defaultValue = "0",
      description =
          "The offset to use when listing workspaces. (Zero means to start from the beginning.)")
  private int offset;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    AuthenticationManager authenticationManager =
        new AuthenticationManager(globalContext, workspaceContext);
    authenticationManager.loginTerraUser();
    java.util.List<WorkspaceDescription> workspaces =
        new WorkspaceManager(globalContext, workspaceContext).listWorkspaces(offset);

    for (WorkspaceDescription workspace : workspaces) {
      String prefix =
          (!workspaceContext.isEmpty()
                  && workspaceContext.getWorkspaceId().equals(workspace.getId()))
              ? " * "
              : "   ";
      System.out.println(prefix + workspace.getId());
    }
    return 0;
  }
}
