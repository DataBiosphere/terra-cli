package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.IamRole;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace add-user" command. */
@Command(name = "add-user", description = "Add a user to the workspace.")
public class AddUser implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "user email")
  private String userEmail;

  @CommandLine.Parameters(index = "1", description = "Role to grant: ${COMPLETION-CANDIDATES}")
  private IamRole role;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    new WorkspaceManager(globalContext, workspaceContext).addUserToWorkspace(userEmail, role);
    System.out.println("User added to workspace: " + userEmail + ", " + role);
    return 0;
  }
}
