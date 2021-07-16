package bio.terra.cli.command.group;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFGroup;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group list" command. */
@Command(name = "list", description = "List the groups to which the current user belongs.")
public class List extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** List the groups to which the current user belongs. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        UserIO.sortAndMap(Group.list(), Comparator.comparing(Group::getName), UFGroup::new),
        List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<UFGroup> returnValue) {
    for (UFGroup group : returnValue) {
      group.print();
    }
  }
}
