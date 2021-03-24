package bio.terra.cli.command.groups;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.utils.SamService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups add-user" command. */
@Command(name = "add-user", description = "Add a user to a group with a given policy.")
public class AddUser extends BaseCommand {
  @CommandLine.Parameters(index = "0", description = "The email of the user.")
  private String user;

  @CommandLine.Option(names = "--group", required = true, description = "The name of the group")
  private String group;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "The name of the policy: ${COMPLETION-CANDIDATES}")
  private SamService.GroupPolicy policy;

  /** Add a user to a Terra group. */
  @Override
  protected void execute() {
    new SamService(globalContext.server, globalContext.requireCurrentTerraUser())
        .addUserToGroup(group, policy, user);
    OUT.println("User " + user + " successfully added to group " + group + ".");
  }
}
