package bio.terra.cli.command.group;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GroupMember;
import bio.terra.cli.serialization.userfacing.UFGroupMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group add-user" command. */
@Command(name = "add-user", description = "Add a user to a group with a given policy.")
public class AddUser extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(AddUser.class);
  @CommandLine.Mixin GroupMember groupMemberOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a user to a Terra group. */
  @Override
  protected void execute() {
    logger.debug(
        "terra group add-user --name={} --policy={}",
        groupMemberOption.email,
        groupMemberOption.policy);
    Group.Member groupMember =
        Group.get(groupMemberOption.groupNameOption.name)
            .addPolicyToMember(groupMemberOption.email, groupMemberOption.policy);
    formatOption.printReturnValue(new UFGroupMember(groupMember), AddUser::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGroupMember returnValue) {
    OUT.println("User added to Terra group.");
    returnValue.print();
  }
}
