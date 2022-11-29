package bio.terra.cli.command.spend;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSpendProfileUser;
import org.apache.commons.lang3.ObjectUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend enable" command. */
@Command(
    name = "enable",
    description = "Enable use of a spend profile for a user or group.")
public class Enable extends BaseCommand {
  @CommandLine.Mixin bio.terra.cli.command.shared.options.SpendProfileUser spendProfileUserOption;
  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  private static void printText(UFSpendProfileUser returnValue) {
    OUT.println("User enabled on the spend profile.");
    returnValue.print();
  }

  /** Enable access to a spend profile for the given email. */
  @Override
  protected void execute() {
    final String profile =
        ObjectUtils.firstNonNull(
            spendProfileUserOption.spendProfile, Context.getServer().getWsmDefaultSpendProfile());

    SpendProfileUser spendProfileUser =
        SpendProfileUser.enable(
            spendProfileUserOption.email, spendProfileUserOption.policy, profile);
    formatOption.printReturnValue(new UFSpendProfileUser(spendProfileUser), Enable::printText);
  }
}
