package bio.terra.cli.command.spend;

import bio.terra.cli.businessobject.SpendProfileUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFSpendProfileUser;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend list-users" command. */
@Command(
    name = "list-users",
    description = "List the users enabled on the Workspace Manager default spend profile.")
public class ListUsers extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(ListUsers.class);
  @CommandLine.Mixin Format formatOption;

  /** List all users that have access to the WSM default spend profile. */
  @Override
  protected void execute() {
    logger.debug("terra spend list-users");
    formatOption.printReturnValue(
        UserIO.sortAndMap(
            SpendProfileUser.list(),
            Comparator.comparing(SpendProfileUser::getEmail),
            UFSpendProfileUser::new),
        ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(List<UFSpendProfileUser> returnValue) {
    for (UFSpendProfileUser spendProfileUser : returnValue) {
      spendProfileUser.print();
    }
  }
}
