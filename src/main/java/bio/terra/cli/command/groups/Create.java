package bio.terra.cli.command.groups;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GroupName;
import bio.terra.cli.serialization.userfacing.UFGroup;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups create" command. */
@Command(name = "create", description = "Create a new Terra group.")
public class Create extends BaseCommand {
  @CommandLine.Mixin GroupName groupNameOption;
  @CommandLine.Mixin Format formatOption;

  /** Create a new Terra group. */
  @Override
  protected void execute() {
    Group group = Group.create(groupNameOption.name);
    formatOption.printReturnValue(new UFGroup(group), Create::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFGroup returnValue) {
    OUT.println("Terra group created.");
    returnValue.print();
  }
}
