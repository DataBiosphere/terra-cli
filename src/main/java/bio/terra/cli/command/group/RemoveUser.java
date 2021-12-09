package bio.terra.cli.command.group;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.GroupMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group remove-user" command. */
@Command(name = "remove-user", description = "Remove a user from a group with a given policy.")
public class RemoveUser extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(RemoveUser.class);
  @CommandLine.Mixin GroupMember groupMemberOption;

  /** Remove a user from a Terra group. */
  @Override
  protected void execute() {
    logger.debug(
        "terra group remove-user --email={} --policy={}",
        groupMemberOption.email,
        groupMemberOption.policy);
    Group.get(groupMemberOption.groupNameOption.name)
        .removePolicyFromMember(groupMemberOption.email, groupMemberOption.policy);
    OUT.println(
        "User ("
            + groupMemberOption.email
            + ") removed from policy ("
            + groupMemberOption.policy
            + ") in group ("
            + groupMemberOption.groupNameOption.name
            + ").");
  }
}
