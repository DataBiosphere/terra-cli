package bio.terra.cli.command.user;

import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.businessobject.TerraUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.serialization.userfacing.UFSpendProfileUser;
import bio.terra.cli.service.SpendProfileManagerService;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra user invite" command. */
@Command(name = "invite", description = "Invite a new user.")
public class Invite extends BaseCommand {
  @CommandLine.Mixin bio.terra.cli.command.shared.options.SpendProfile spendProfileOption;

  @CommandLine.Option(names = "--email", required = true, description = "User email.")
  private String email;

  @CommandLine.Option(
      names = "--enable-spend",
      defaultValue = "false",
      description = "Also enable the email as a user on the default spend profile.")
  private boolean enableSpend;

  /** Invite a new user. */
  @Override
  protected void execute() {
    TerraUser.invite(email);
    OUT.println("Successfully invited user.");

    if (enableSpend) {
      SpendProfileUser spendProfileUser =
          SpendProfileUser.enable(
              email,
              SpendProfileManagerService.SpendProfilePolicy.USER,
              spendProfileOption.spendProfile,
              /*saveToUserProfile=*/ true);
      OUT.println("User enabled on the spend profile.");
      new UFSpendProfileUser(spendProfileUser).print();
    }
  }
}
