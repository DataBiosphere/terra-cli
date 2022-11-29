package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.WorkspaceUser;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFWorkspaceUser;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace add-user" command. */
@Command(name = "add-user", description = "Add a user or group to the workspace.")
public class AddUser extends WsmBaseCommand {

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(names = "--email", required = true, description = "User or group email.")
  private String email;

  @CommandLine.Option(
      names = "--role",
      required = true,
      description = "Role to grant: ${COMPLETION-CANDIDATES}.")
  private WorkspaceUser.Role role;

  /** Print this command's output in text format. */
  private static void printText(UFWorkspaceUser returnValue) {
    OUT.println("User added to workspace.");
    returnValue.print();
  }

  /** Add an email to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    WorkspaceUser workspaceUser = WorkspaceUser.add(email, role, Context.requireWorkspace());
    formatOption.printReturnValue(new UFWorkspaceUser(workspaceUser), AddUser::printText);
  }
}
