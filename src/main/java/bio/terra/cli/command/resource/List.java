package bio.terra.cli.command.resource;

import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.app.utils.tables.UFResourceColumns;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.workspace.model.StewardshipType;
import java.util.Comparator;
import java.util.stream.Collectors;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource list" command. */
@CommandLine.Command(name = "list", description = "List all resources.")
public class List extends BaseCommand {
  @CommandLine.Option(
      names = "--stewardship",
      description = "Filter on a particular stewardship type: ${COMPLETION-CANDIDATES}.")
  private StewardshipType stewardship;

  @CommandLine.Option(
      names = "--type",
      description = "Filter on a particular resource type: ${COMPLETION-CANDIDATES}.")
  private Resource.Type type;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

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

  /** Print this command's output in tabular text format. */
  private static void printText(java.util.List<UFResource> returnValue) {
    TablePrinter<UFResource> printer = UFResourceColumns::values;
    OUT.println(printer.print(returnValue));
  }
}
