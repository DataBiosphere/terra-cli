package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.WorkspaceUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
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
      description = "Role to grant: ${COMPLETION-CANDIDATES}.")
  private WorkspaceUser.Role role;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** Remove a user from a workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    WorkspaceUser.remove(email, role, Context.requireWorkspace());
    OUT.println("User (" + email + ") removed from workspace role (" + role + ").");
  }
}
