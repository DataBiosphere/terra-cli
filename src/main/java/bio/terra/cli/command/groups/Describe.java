package bio.terra.cli.command.groups;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GroupName;
import bio.terra.cli.serialization.userfacing.UFGroup;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups describe" command. */
@Command(name = "describe", description = "Print the group email address.")
public class Describe extends BaseCommand {
  @CommandLine.Mixin GroupName groupNameOption;
  @CommandLine.Mixin Format formatOption;

  /** Describe an existing Terra group. */
  @Override
  protected void execute() {
    Group group = Group.get(groupNameOption.name);
    formatOption.printReturnValue(new UFGroup(group), Describe::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGroup returnValue) {
    returnValue.print();
  }
}
