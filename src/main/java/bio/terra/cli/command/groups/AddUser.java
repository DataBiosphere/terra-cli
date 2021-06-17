package bio.terra.cli.command.groups;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GroupMember;
import bio.terra.cli.serialization.userfacing.UFGroupMember;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups add-user" command. */
@Command(name = "add-user", description = "Add a user to a group with a given policy.")
public class AddUser extends BaseCommand {
  @CommandLine.Mixin GroupMember groupMemberOption;
  @CommandLine.Mixin Format formatOption;

  /** Add a user to a Terra group. */
  @Override
  protected void execute() {
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
