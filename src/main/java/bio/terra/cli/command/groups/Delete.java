package bio.terra.cli.command.groups;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.utils.SamService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups delete" command. */
@Command(name = "delete", description = "Delete an existing Terra group.")
public class Delete extends BaseCommand {
  @CommandLine.Parameters(index = "0", description = "The name of the group")
  private String group;

  /** Delete an existing Terra group. */
  @Override
  protected void execute() {
    new SamService(globalContext.server, globalContext.requireCurrentTerraUser())
        .deleteGroup(group);
    OUT.println("Group " + group + " successfully deleted.");
  }
}
