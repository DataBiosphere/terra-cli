package bio.terra.cli.command.spend;

import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSpendProfileUser;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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
    List<SpendProfileUser> spendProfileUsers = SpendProfileUser.list();
    formatOption.printReturnValue(
        spendProfileUsers.stream()
            .sorted(Comparator.comparing(SpendProfileUser::getEmail))
            .map(spendProfileUser -> new UFSpendProfileUser(spendProfileUser))
            .collect(Collectors.toList()),
        ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(List<UFSpendProfileUser> returnValue) {
    for (UFSpendProfileUser spendProfileUser : returnValue) {
      spendProfileUser.print();
    }
  }
}
