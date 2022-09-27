package bio.terra.cli.command.resource;

import static bio.terra.cli.app.utils.tables.ColumnDefinition.Alignment.LEFT;

import bio.terra.cli.app.utils.tables.ColumnDefinition;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.workspace.model.StewardshipType;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Collectors;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource list" command. */
@CommandLine.Command(name = "list", description = "List all resources.")
public class List extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  @CommandLine.Option(
      names = "--stewardship",
      description = "Filter on a particular stewardship type: ${COMPLETION-CANDIDATES}.")
  private StewardshipType stewardship;

  @CommandLine.Option(
      names = "--type",
      description = "Filter on a particular resource type: ${COMPLETION-CANDIDATES}.")
  private Resource.Type type;

  /** Print this command's output in tabular text format. */
  private static void printText(java.util.List<UFResource> returnValue) {
    TablePrinter<UFResource> printer = UFResourceColumns::values;
    OUT.println(printer.print(returnValue));
  }

  /** List the resources in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    java.util.List<UFResource> resources =
        Context.requireWorkspace().listResourcesAndSync().stream()
            .filter(
                (resource) -> {
                  boolean stewardshipMatches =
                      stewardship == null || resource.getStewardshipType().equals(stewardship);
                  boolean typeMatches = type == null || resource.getResourceType().equals(type);
                  return stewardshipMatches && typeMatches;
                })
            .sorted(Comparator.comparing(Resource::getName))
            .map(Resource::serializeToCommand)
            .collect(Collectors.toList());
    formatOption.printReturnValue(resources, List::printText);
  }

  /** Column information for fields in `resource list` output */
  private enum UFResourceColumns implements ColumnDefinition<UFResource> {
    NAME("NAME", r -> r.name, 45, LEFT),
    RESOURCE_TYPE("RESOURCE TYPE", r -> r.resourceType.toString(), 30, LEFT),
    STEWARDSHIP_TYPE("STEWARDSHIP TYPE", r -> r.stewardshipType.toString(), 20, LEFT),
    DESCRIPTION("DESCRIPTION", r -> r.description, 40, LEFT);

    private final String columnLabel;
    private final Function<UFResource, String> valueExtractor;
    private final int width;
    private final Alignment alignment;

    UFResourceColumns(
        String columnLabel,
        Function<UFResource, String> valueExtractor,
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
    public Function<UFResource, String> getValueExtractor() {
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
