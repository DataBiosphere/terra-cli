package bio.terra.cli.command.groups;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.GroupMemberProperties;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups remove-user" command. */
@Command(name = "remove-user", description = "Remove a user from a group with a given policy.")
public class RemoveUser extends BaseCommand {
  @CommandLine.Mixin GroupMemberProperties groupMemberOption;

  /** Remove a user from a Terra group. */
  @Override
  protected void execute() {
    Group.get(groupMemberOption.name)
        .removePolicyFromMember(groupMemberOption.email, groupMemberOption.policy);
    OUT.println(
        "User ("
            + groupMemberOption.email
            + ") removed from policy ("
            + groupMemberOption.policy
            + ") in group ("
            + groupMemberOption.name
            + ").");
  }
}
