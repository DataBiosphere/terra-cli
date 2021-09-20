package bio.terra.cli.command.group;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GroupName;
import bio.terra.cli.serialization.userfacing.UFGroup;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group delete" command. */
@Command(name = "delete", description = "Delete an existing Terra group.")
public class Delete extends BaseCommand {
  @CommandLine.Mixin DeletePrompt deletePromptOption;
  @CommandLine.Mixin GroupName groupNameOption;
  @CommandLine.Mixin Format formatOption;

  /** Delete an existing Terra group. */
  @Override
  protected void execute() {
    Group group = Group.get(groupNameOption.name);
    UFGroup ufGroup = new UFGroup(group);
    ufGroup.print();
    deletePromptOption.confirmOrThrow();
    group.delete();
    formatOption.printReturnValue("Terra group deleted.");
  }
}
