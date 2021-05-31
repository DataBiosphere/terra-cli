package bio.terra.cli.command.spend;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.SpendProfileManagerService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend enable" command. */
@Command(
    name = "enable",
    description = "Enable use of the Workspace Manager default spend profile for a user or group.")
public class Enable extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "The email of the user or group.")
  private String email;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "The name of the policy: ${COMPLETION-CANDIDATES}")
  private SpendProfileManagerService.SpendProfilePolicy policy;

  /** Enable access to the WSM default spend profile for the given email. */
  @Override
  protected void execute() {
    new SpendProfileManagerService(
            globalContext.getServer(), globalContext.requireCurrentTerraUser())
        .enableUserForDefaultSpendProfile(policy, email);

    OUT.println(
        "Email " + email + " successfully enabled on the Workspace Manager default spend profile.");
  }
}
