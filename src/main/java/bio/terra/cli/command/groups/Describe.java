package bio.terra.cli.command.groups;

import bio.terra.cli.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.service.SamService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups describe" command. */
@Command(name = "describe", description = "Print the group email address.")
public class Describe extends BaseCommand {
  @CommandLine.Parameters(index = "0", description = "The name of the group")
  private String group;

  @CommandLine.Mixin Format formatOption;

  /** Describe an existing Terra group. */
  @Override
  protected void execute() {
    String groupEmail =
        new SamService(Context.getServer(), Context.requireUser()).getGroupEmail(group);
    formatOption.printReturnValue(groupEmail);
  }
}
