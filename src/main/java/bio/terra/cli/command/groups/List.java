package bio.terra.cli.command.groups;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.service.SamService;
import org.broadinstitute.dsde.workbench.client.sam.model.ManagedGroupMembershipEntry;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups list" command. */
@Command(name = "list", description = "List the groups to which the current user belongs.")
public class List extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** List the groups to which the current user belongs. */
  @Override
  protected void execute() {
    java.util.List<ManagedGroupMembershipEntry> groups =
        new SamService(Context.getServer(), Context.requireUser()).listGroups();
    formatOption.printReturnValue(groups, List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<ManagedGroupMembershipEntry> returnValue) {
    for (ManagedGroupMembershipEntry group : returnValue) {
      OUT.println(group.getGroupName());
    }
  }
}
