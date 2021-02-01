package bio.terra.cli.command.workspace;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.WorkspaceManager;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import bio.terra.workspace.model.IamRole;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace add-user" command. */
@Command(name = "add-user", description = "Add a user to the workspace.")
public class AddUser implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "user email")
  private String userEmail;

  @CommandLine.Parameters(index = "1", description = "IAM role to grant: ${COMPLETION-CANDIDATES}")
  private IamRole iamRole;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext).requireCurrentTerraUser();
    new WorkspaceManager(globalContext, workspaceContext).addUserToWorkspace(userEmail, iamRole);
    System.out.println(
        "User successfully added to workspace. (" + userEmail + " : " + iamRole + ")");
    return 0;
  }
}
