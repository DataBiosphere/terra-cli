package bio.terra.cli.command.groups;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.SamService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups remove-user" command. */
@Command(name = "remove-user", description = "Remove a user from a group with a given policy.")
public class RemoveUser extends BaseCommand {
  @CommandLine.Parameters(index = "0", description = "The email of the user.")
  private String user;

  @CommandLine.Option(names = "--group", required = true, description = "The name of the group")
  private String group;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "The name of the policy: ${COMPLETION-CANDIDATES}")
  private SamService.GroupPolicy policy;

  /** Delete an existing Terra group. */
  @Override
  protected void execute() {
    new SamService(globalContext.getServer(), globalContext.requireCurrentTerraUser())
        .removeUserFromGroup(group, policy, user);
    OUT.println("User " + user + " successfully removed from group " + group + ".");
  }
}
