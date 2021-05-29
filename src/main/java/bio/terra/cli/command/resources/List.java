package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.Resource;
import bio.terra.workspace.model.ResourceType;
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
  private ResourceType type;

  @CommandLine.Mixin Format formatOption;

  /** List the resources in the workspace. */
  @Override
  protected void execute() {
    java.util.List<Resource> resources =
        Resource.listAndSync().stream()
            .filter(
                (resource) -> {
                  boolean stewardshipMatches =
                      stewardship == null || resource.stewardshipType.equals(stewardship);
                  boolean typeMatches = type == null || resource.resourceType.equals(type);
                  return stewardshipMatches && typeMatches;
                })
            .collect(Collectors.toList());
    formatOption.printReturnValue(resources, List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<Resource> returnValue) {
    for (Resource resource : returnValue) {
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
