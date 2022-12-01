package bio.terra.cli.command.workspace;

import static bio.terra.cli.app.utils.tables.ColumnDefinition.Alignment.LEFT;

import bio.terra.cli.app.utils.tables.ColumnDefinition;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.userfacing.UFWorkspaceLight;
import bio.terra.cli.utils.UserIO;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.Predicate;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace list" command. */
@Command(
    name = "list",
    description = "List all workspaces the current user can access.",
    showDefaultValues = true)
public class List extends BaseCommand {
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--offset",
      required = false,
      defaultValue = "0",
      description =
          "The offset to use when listing workspaces. (Zero means to start from the beginning.)")
  private int offset;

  @CommandLine.Option(
      names = "--limit",
      required = false,
      defaultValue = "30",
      description = "The maximum number of workspaces to return.")
  private int limit;

  /** List all workspaces a user has access to. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        UserIO.sortAndMap(
            Workspace.list(offset, limit),
            Comparator.comparing(Workspace::getName),
            UFWorkspaceLight::new),
        this::printText);
  }

  /** Print this command's output in tabular text format. */
  private void printText(java.util.List<UFWorkspaceLight> returnValue) {
    // Guard against the current workspace being empty, but keep the highlight column so the
    // table is formatted the same with or without the workspace being set (i.e. pass always-false
    // instead of a null predicate).
    Predicate<UFWorkspaceLight> isHighlighted =
        Context.getWorkspace()
            .map(
                current ->
                    (Predicate<UFWorkspaceLight>) (ufw -> current.getUserFacingId().equals(ufw.id)))
            .orElse(ufw -> false);
    TablePrinter<UFWorkspaceLight> printer = Columns::values;
    String text = printer.print(returnValue, isHighlighted);
    OUT.println(text);
  }

  /** Column information for table output with `terra workspace list` */
  private enum Columns implements ColumnDefinition<UFWorkspaceLight> {
    ID("ID", w -> w.id, 40, LEFT),
    NAME("NAME", w -> w.name, 30, LEFT),
    GOOGLE_PROJECT("GOOGLE PROJECT", w -> w.googleProjectId, 15, LEFT),
    AWS_ACCOUNT("AWS_ACCOUNT", w -> w.awsAccountNumber, 15, LEFT),
    LANDING_ZONE_ID("LANDING_ZONE_ID", w -> w.landingZoneId, 40, LEFT),
    DESCRIPTION("DESCRIPTION", w -> w.description, 40, LEFT);

    private final String columnLabel;
    private final Function<UFWorkspaceLight, String> valueExtractor;
    private final int width;
    private final Alignment alignment;

    Columns(
        String columnLabel,
        Function<UFWorkspaceLight, String> valueExtractor,
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
    public Function<UFWorkspaceLight, String> getValueExtractor() {
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
