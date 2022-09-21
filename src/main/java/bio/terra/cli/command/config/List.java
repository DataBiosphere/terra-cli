package bio.terra.cli.command.config;

import static bio.terra.cli.app.utils.tables.ColumnDefinition.Alignment.LEFT;

import bio.terra.cli.app.utils.tables.ColumnDefinition;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFConfig;
import bio.terra.cli.serialization.userfacing.UFConfigItem;
import java.util.function.Function;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra config list" command. */
@CommandLine.Command(
    name = "list",
    description = "List all configuration properties and their values.")
public class List extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Print this command's output in text format. */
  private static void printText(java.util.List<UFConfigItem> returnValue) {
    TablePrinter<UFConfigItem> printer =
        bio.terra.cli.command.config.List.UFConfigItemColumns::values;
    OUT.println(printer.print(returnValue));
  }

  /** Print out a list of all the config properties. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        new UFConfig(Context.getConfig(), Context.getServer(), Context.getWorkspace()).items,
        List::printText);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }

  /** Column information for table output with `terra config list` */
  private enum UFConfigItemColumns implements ColumnDefinition<UFConfigItem> {
    OPTION("OPTION", w -> w.option, 20, LEFT),
    VALUE("VALUE", w -> w.value, 45, LEFT),
    DESCRIPTION("DESCRIPTION", w -> w.description, 60, LEFT);

    private final String columnLabel;
    private final Function<UFConfigItem, String> valueExtractor;
    private final int width;
    private final Alignment alignment;

    UFConfigItemColumns(
        String columnLabel,
        Function<UFConfigItem, String> valueExtractor,
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
    public Function<UFConfigItem, String> getValueExtractor() {
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
