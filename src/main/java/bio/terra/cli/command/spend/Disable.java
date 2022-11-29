package bio.terra.cli.command.spend;

import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.command.shared.BaseCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend disable" command. */
@Command(name = "disable", description = "Disable use of a spend profile for a user or group.")
public class Disable extends BaseCommand {
  @CommandLine.Mixin bio.terra.cli.command.shared.options.SpendProfileUser spendProfileUserOption;

  /** Remove access to a spend profile for the given email. */
  @Override
  protected void execute() {
    SpendProfileUser.disable(
        spendProfileUserOption.email,
        spendProfileUserOption.policy,
        spendProfileUserOption.spendProfile);
    OUT.println(
        "User ("
            + spendProfileUserOption.email
            + ") disabled on the ("
            + spendProfileUserOption.spendProfile
            + ") spend profile ("
            + spendProfileUserOption.policy
            + ").");
  }
}
