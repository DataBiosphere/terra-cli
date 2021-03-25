package bio.terra.cli.command.workspace;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.IamRole;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace remove-user" command. */
@Command(name = "remove-user", description = "Remove a user or group from the workspace.")
public class RemoveUser extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "user or group email")
  private String userEmail;

  @CommandLine.Parameters(index = "1", description = "Role to remove: ${COMPLETION-CANDIDATES}")
  private IamRole role;

  /** Remove a user from a workspace. */
  @Override
  protected void execute() {
    new WorkspaceManager(globalContext, workspaceContext).removeUserFromWorkspace(userEmail, role);
    OUT.println("Email removed from workspace: " + userEmail + ", " + role);
  }
}
