package bio.terra.cli.command.spend;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.service.SpendProfileManagerService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend disable" command. */
@Command(
    name = "disable",
    description = "Disable use of the Workspace Manager default spend profile for a user or group.")
public class Disable extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "The email of the user or group.")
  private String email;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "The name of the policy: ${COMPLETION-CANDIDATES}")
  private SpendProfileManagerService.SpendProfilePolicy policy;

  /** Remove access to the WSM default spend profile for the given email. */
  @Override
  protected void execute() {
    new SpendProfileManagerService(Context.getServer(), Context.requireUser())
        .disableUserForDefaultSpendProfile(policy, email);

    OUT.println(
        "Email "
            + email
            + " successfully disabled on the Workspace Manager default spend profile.");
  }
}
