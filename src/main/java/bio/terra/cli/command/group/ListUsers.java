package bio.terra.cli.command.group;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GroupName;
import bio.terra.cli.serialization.userfacing.UFGroupMember;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group list-users" command. */
@Command(name = "list-users", description = "List the users in a group.")
public class ListUsers extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(ListUsers.class);
  @CommandLine.Mixin GroupName groupNameOption;

  @CommandLine.Mixin Format formatOption;

  /** List the users in the given group. */
  @Override
  protected void execute() {
    logger.debug("terra group list-users --name=" + groupNameOption.name);
    formatOption.printReturnValue(
        UserIO.sortAndMap(
            Group.get(groupNameOption.name).getMembers(),
            Comparator.comparing(Group.Member::getEmail),
            UFGroupMember::new),
        ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(List<UFGroupMember> returnValue) {
    for (UFGroupMember groupMember : returnValue) {
      groupMember.print();
    }
  }
}
