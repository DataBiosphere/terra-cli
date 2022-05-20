package bio.terra.cli.command.group;

import static bio.terra.cli.app.utils.tables.ColumnDefinition.Alignment.LEFT;
import static bio.terra.cli.app.utils.tables.ColumnDefinition.Alignment.RIGHT;

import bio.terra.cli.app.utils.tables.ColumnDefinition;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFGroup;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import java.util.function.Function;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group list" command. */
@Command(name = "list", description = "List the groups to which the current user belongs.")
public class List extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  /** List the groups to which the current user belongs. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        UserIO.sortAndMap(Group.list(), Comparator.comparing(Group::getName), UFGroup::new),
        List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<UFGroup> returnValue) {
    TablePrinter<UFGroup> printer = bio.terra.cli.command.group.List.UFGroupColumns::values;
    OUT.println(printer.print(returnValue));
  }

  /** Column information for fields in `resource list` output */
  private enum UFGroupColumns implements ColumnDefinition<UFGroup> {
    NAME("NAME", g -> g.name, 30, LEFT),
    EMAIL("EMAIL", g -> g.email, 45, LEFT),
    MEMBERS("MEMBERS", g -> g.numMembers.toString(), 7, RIGHT),
    POLICIES("POLICIES", g -> g.currentUserPolicies.toString(), 15, LEFT);

    private final String columnLabel;
    private final Function<UFGroup, String> valueExtractor;
    private final int width;
    private final Alignment alignment;

    UFGroupColumns(
        String columnLabel,
        Function<UFGroup, String> valueExtractor,
        int width,
        Alignment alignment) {
      this.columnLabel = columnLabel;
      this.valueExtractor = valueExtractor;
      this.width = width;
      this.alignment = alignment;
    }

    @Override
    public String getLabel() {
      return columnLabel;
    }

    @Override
    public Function<UFGroup, String> getValueExtractor() {
      return valueExtractor;
    }

    @Override
    public int getWidth() {
      return width;
    }

    @Override
    public Alignment getAlignment() {
      return alignment;
    }
  }
}
