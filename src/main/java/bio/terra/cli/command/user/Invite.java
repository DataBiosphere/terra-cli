package bio.terra.cli.command.user;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.businessobject.TerraUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.serialization.userfacing.UFSpendProfileUser;
import bio.terra.cli.service.SpendProfileManagerService;
import java.util.Optional;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra user invite" command. */
@Command(name = "invite", description = "Invite a new user.")
public class Invite extends BaseCommand {

  @CommandLine.Option(names = "--email", required = true, description = "User email.")
  private String email;

  @CommandLine.Option(
      names = "--enable-spend",
      arity = "0..1",
      description =
          "Also enable the email as a user on a spend profile, or the default if none is specified.")
  private Optional<String> spendProfile;

  /** Invite a new user. */
  @Override
  protected void execute() {
    TerraUser.invite(email);
    OUT.println("Successfully invited user.");

    if (spendProfile.isPresent()) {
      final String profile =
          spendProfile.get().isEmpty()
              ? Context.getServer().getWsmDefaultSpendProfile()
              : spendProfile.get();

      SpendProfileUser spendProfileUser =
          SpendProfileUser.enable(
              email, SpendProfileManagerService.SpendProfilePolicy.USER, profile);
      OUT.println("User enabled on the spend profile.");
      new UFSpendProfileUser(spendProfileUser).print();
    }
  }
}
