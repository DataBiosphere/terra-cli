package bio.terra.cli.command.spend;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.command.shared.BaseCommand;
import org.apache.commons.lang3.ObjectUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend disable" command. */
@Command(
    name = "disable",
    description = "Disable use of a spend profile for a user or group.")
public class Disable extends BaseCommand {
  @CommandLine.Mixin bio.terra.cli.command.shared.options.SpendProfileUser spendProfileUserOption;

  /** Remove access to a spend profile for the given email. */
  @Override
  protected void execute() {
    final String profile =
        ObjectUtils.firstNonNull(
            spendProfileUserOption.spendProfile, Context.getServer().getWsmDefaultSpendProfile());

    SpendProfileUser.disable(spendProfileUserOption.email, spendProfileUserOption.policy, profile);
    OUT.println(
        "User ("
            + spendProfileUserOption.email
            + ") disabled on the ("
            + profile
            + ") spend profile ("
            + spendProfileUserOption.policy
            + ").");
  }
}
