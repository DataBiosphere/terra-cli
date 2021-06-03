package bio.terra.cli.command.resources;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandResource;
import bio.terra.workspace.model.StewardshipType;
import java.util.stream.Collectors;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources list" command. */
@CommandLine.Command(name = "list", description = "List all resources.")
public class List extends BaseCommand {
  @CommandLine.Option(
      names = "--stewardship",
      description = "Filter on a particular stewardship type: ${COMPLETION-CANDIDATES}")
  private StewardshipType stewardship;

  @CommandLine.Option(
      names = "--type",
      description = "Filter on a particular resource type: ${COMPLETION-CANDIDATES}")
  private Resource.Type type;

  @CommandLine.Mixin Format formatOption;

  /** List the resources in the workspace. */
  @Override
  protected void execute() {
    java.util.List<CommandResource> resources =
        Context.requireWorkspace().listResourcesAndSync().stream()
            .filter(
                (resource) -> {
                  boolean stewardshipMatches =
                      stewardship == null || resource.getStewardshipType().equals(stewardship);
                  boolean typeMatches = type == null || resource.getResourceType().equals(type);
                  return stewardshipMatches && typeMatches;
                })
            .map(Resource::serializeToCommand)
            .collect(Collectors.toList());
    formatOption.printReturnValue(resources, List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<CommandResource> returnValue) {
    for (CommandResource resource : returnValue) {
      OUT.println(
          resource.name
              + " ("
              + resource.resourceType
              + ", "
              + resource.stewardshipType
              + "): "
              + resource.description);
    }
  }
}
