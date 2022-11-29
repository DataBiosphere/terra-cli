package bio.terra.cli.command.spend;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSpendProfileUser;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend list-users" command. */
@Command(
    name = "list-users",
    description = "List the users enabled on a spend profile.")
public class ListUsers extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(names = "--profile", description = "Name of the spend profile.")
  private String spendProfile;

  /** Print this command's output in text format. */
  private static void printText(List<UFSpendProfileUser> returnValue) {
    for (UFSpendProfileUser spendProfileUser : returnValue) {
      spendProfileUser.print();
    }
  }

  /** List all users that have access to a spend profile. */
  @Override
  protected void execute() {
    final String profile =
        ObjectUtils.firstNonNull(spendProfile, Context.getServer().getWsmDefaultSpendProfile());

    formatOption.printReturnValue(
        UserIO.sortAndMap(
            SpendProfileUser.list(profile),
            Comparator.comparing(SpendProfileUser::getEmail),
            UFSpendProfileUser::new),
        ListUsers::printText);
  }
}
