package bio.terra.cli.command.workspace;

import bio.terra.cli.WorkspaceUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandWorkspaceUser;
import bio.terra.workspace.model.IamRole;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace remove-user" command. */
@Command(name = "remove-user", description = "Remove a user or group from the workspace.")
public class RemoveUser extends BaseCommand {

  @CommandLine.Option(names = "--email", required = true, description = "User or group email.")
  private String email;

  @CommandLine.Option(
      names = "--role",
      required = true,
      description = "Role to grant: ${COMPLETION-CANDIDATES}")
  private IamRole role;

  @CommandLine.Mixin Format formatOption;

  /** Remove a user from a workspace. */
  @Override
  protected void execute() {
    WorkspaceUser workspaceUser = WorkspaceUser.remove(email, role);
    formatOption.printReturnValue(new CommandWorkspaceUser(workspaceUser), RemoveUser::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(CommandWorkspaceUser returnValue) {
    OUT.println("Email + role removed from workspace.");
    returnValue.print();
  }
}
