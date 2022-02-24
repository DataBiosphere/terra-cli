package bio.terra.cli.command.server;

import static bio.terra.cli.app.utils.tables.PrintableColumn.Alignment.LEFT;

import bio.terra.cli.app.utils.tables.PrintableColumn;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFServer;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import java.util.function.Function;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server list" command. */
@Command(name = "list", description = "List all available Terra servers.")
public class List extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** List all Terra environments. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        UserIO.sortAndMap(Server.list(), Comparator.comparing(Server::getName), UFServer::new),
        this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(java.util.List<UFServer> returnValue) {
    Server currentServer = Context.getServer();
    TablePrinter<UFServer> printer = Columns::values;
    // print the UFServers, and highlight the current one
    String text = printer.print(returnValue, s -> currentServer.getName().equals(s.name));
    OUT.println(text);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }

  /** Column information for table output with `terra server list` */
  private enum Columns implements PrintableColumn<UFServer> {
    NAME("NAME", s -> s.name, 30, LEFT),
    DESCRIPTION("DESCRIPTION", s -> s.description, 90, LEFT);

    private final String columnLabel;
    private final Function<UFServer, String> valueExtractor;
    private final int width;
    private final Alignment alignment;

    Columns(
        String columnLabel,
        Function<UFServer, String> valueExtractor,
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
    public Function<UFServer, String> getValueExtractor() {
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
