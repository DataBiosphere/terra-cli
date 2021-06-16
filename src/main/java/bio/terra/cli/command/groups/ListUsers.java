package bio.terra.cli.command.groups;

import bio.terra.cli.businessobject.GroupMember;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GroupName;
import bio.terra.cli.serialization.userfacing.UFGroupMember;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups list-users" command. */
@Command(name = "list-users", description = "List the users in a group.")
public class ListUsers extends BaseCommand {
  @CommandLine.Mixin GroupName groupNameOption;

  @CommandLine.Mixin Format formatOption;

  /** List the users in the given group. */
  @Override
  protected void execute() {
    List<GroupMember> groupMembers = GroupMember.list(groupNameOption.name);
    formatOption.printReturnValue(
        groupMembers.stream()
            .sorted(Comparator.comparing(GroupMember::getEmail))
            .map(groupMember -> new UFGroupMember(groupMember))
            .collect(Collectors.toList()),
        ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(List<UFGroupMember> returnValue) {
    for (UFGroupMember groupMember : returnValue) {
      groupMember.print();
    }
  }
}
