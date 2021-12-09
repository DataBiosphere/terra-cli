package bio.terra.cli.command.group;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.DeletePrompt;
import bio.terra.cli.command.shared.options.GroupName;
import bio.terra.cli.serialization.userfacing.UFGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group delete" command. */
@Command(name = "delete", description = "Delete an existing Terra group.")
public class Delete extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Delete.class);
  @CommandLine.Mixin DeletePrompt deletePromptOption;
  @CommandLine.Mixin GroupName groupNameOption;

  /** Delete an existing Terra group. */
  @Override
  protected void execute() {
    logger.debug("terra group delete --name=" + groupNameOption.name);
    Group groupToDelete = Group.get(groupNameOption.name);

    // print details about the group before showing the delete prompt
    new UFGroup(groupToDelete).print();
    deletePromptOption.confirmOrThrow();

    groupToDelete.delete();
    OUT.println("Terra group successfully deleted.");
  }
}
