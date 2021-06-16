package bio.terra.cli.command.groups;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFGroup;
import java.util.Comparator;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups list" command. */
@Command(name = "list", description = "List the groups to which the current user belongs.")
public class List extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** List the groups to which the current user belongs. */
  @Override
  protected void execute() {
    java.util.List<Group> groups = Group.list();
    formatOption.printReturnValue(
        groups.stream()
            .sorted(Comparator.comparing(Group::getName))
            .map(group -> new UFGroup(group))
            .collect(Collectors.toList()),
        List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<UFGroup> returnValue) {
    for (UFGroup group : returnValue) {
      group.print();
    }
  }
}
