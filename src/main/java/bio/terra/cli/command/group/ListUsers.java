package bio.terra.cli.command.group;

import static bio.terra.cli.app.utils.tables.ColumnDefinition.Alignment.LEFT;
import static bio.terra.cli.app.utils.tables.ColumnDefinition.Alignment.RIGHT;

import bio.terra.cli.app.utils.tables.ColumnDefinition;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Group;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.GroupName;
import bio.terra.cli.serialization.userfacing.UFGroupMember;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra group list-users" command. */
@Command(name = "list-users", description = "List the users in a group.")
public class ListUsers extends BaseCommand {
  @CommandLine.Mixin GroupName groupNameOption;

  @CommandLine.Mixin Format formatOption;

  /** List the users in the given group. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        UserIO.sortAndMap(
            Group.get(groupNameOption.name).getMembers(),
            Comparator.comparing(Group.Member::getEmail),
            UFGroupMember::new),
        ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(List<UFGroupMember> returnValue) {
    TablePrinter<UFGroupMember> printer = ListUsers.UFGroupListColumns::values;
    OUT.println(printer.print(returnValue));
  }

  /** Column information for fields in `resource list` output */
  private enum UFGroupListColumns implements ColumnDefinition<UFGroupMember> {
    EMAIL("EMAIL", g -> g.email, 42, LEFT),
    MEMBERS("MEMBERS", g -> g.policies.toString(), 10, LEFT);

    private final String columnLabel;
    private final Function<UFGroupMember, String> valueExtractor;
    private final int width;
    private final Alignment alignment;

    UFGroupListColumns(
        String columnLabel,
        Function<UFGroupMember, String> valueExtractor,
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
    public Function<UFGroupMember, String> getValueExtractor() {
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
