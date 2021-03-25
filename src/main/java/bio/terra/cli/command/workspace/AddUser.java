package bio.terra.cli.command.workspace;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.IamRole;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace add-user" command. */
@Command(name = "add-user", description = "Add a user or group to the workspace.")
public class AddUser extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "user or group email")
  private String userEmail;

  @CommandLine.Parameters(index = "1", description = "Role to grant: ${COMPLETION-CANDIDATES}")
  private IamRole role;

  /** Add an email to the workspace. */
  @Override
  protected void execute() {
    new WorkspaceManager(globalContext, workspaceContext).addUserToWorkspace(userEmail, role);
    OUT.println("Email added to workspace: " + userEmail + ", " + role);
  }
}
