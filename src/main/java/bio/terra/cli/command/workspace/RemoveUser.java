package bio.terra.cli.command.workspace;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.WorkspaceManager;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import bio.terra.workspace.model.IamRole;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace remove-user" command. */
@Command(name = "remove-user", description = "Remove a user from the workspace.")
public class RemoveUser implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "user email")
  private String userEmail;

  @CommandLine.Parameters(index = "1", description = "Role to remove: ${COMPLETION-CANDIDATES}")
  private IamRole role;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    new WorkspaceManager(globalContext, workspaceContext).removeUserFromWorkspace(userEmail, role);
    System.out.println("User removed to workspace: " + userEmail + ", " + role);
    return 0;
  }
}
