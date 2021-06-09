package bio.terra.cli.command.spend;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.service.SpendProfileManagerService;
import java.util.List;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyResponseEntry;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend list-users" command. */
@Command(
    name = "list-users",
    description = "List the users enabled on the Workspace Manager default spend profile.")
public class ListUsers extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** List all users that have access to the WSM default spend profile. */
  @Override
  protected void execute() {
    List<AccessPolicyResponseEntry> policies =
        new SpendProfileManagerService(Context.getServer(), Context.requireUser())
            .listUsersOfDefaultSpendProfile();
    formatOption.printReturnValue(policies, ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(List<AccessPolicyResponseEntry> returnValue) {
    for (AccessPolicyResponseEntry policy : returnValue) {
      OUT.println(policy.getPolicyName().toUpperCase());
      for (String member : policy.getPolicy().getMemberEmails()) {
        OUT.println("  " + member);
      }
    }
  }
}
