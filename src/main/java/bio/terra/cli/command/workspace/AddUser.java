package bio.terra.cli.command.workspace;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.WorkspaceUser;
import bio.terra.workspace.model.IamRole;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace add-user" command. */
@Command(name = "add-user", description = "Add a user or group to the workspace.")
public class AddUser extends BaseCommand {

  @CommandLine.Option(names = "--email", required = true, description = "User or group email.")
  private String email;

  @CommandLine.Option(
      names = "--role",
      required = true,
      description = "Role to grant: ${COMPLETION-CANDIDATES}")
  private IamRole role;

  @CommandLine.Mixin Format formatOption;

  /** Add an email to the workspace. */
  @Override
  protected void execute() {
    WorkspaceUser workspaceUser = WorkspaceUser.add(email, role);
    formatOption.printReturnValue(workspaceUser, AddUser::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(WorkspaceUser returnValue) {
    OUT.println("Email + role added to workspace.");
    returnValue.printText();
  }
}
