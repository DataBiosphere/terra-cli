package bio.terra.cli.command.group;

import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFGroup;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group list" command. */
@Command(name = "list", description = "List the groups to which the current user belongs.")
public class List extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(List.class);
  @CommandLine.Mixin Format formatOption;

  /** List the groups to which the current user belongs. */
  @Override
  protected void execute() {
    logger.debug("terra group list");
    formatOption.printReturnValue(
        UserIO.sortAndMap(Group.list(), Comparator.comparing(Group::getName), UFGroup::new),
        List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<UFGroup> returnValue) {
    for (UFGroup group : returnValue) {
      group.print();
    }
  }
}
