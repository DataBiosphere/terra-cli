package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
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
    java.util.List<ResourceDescription> resources =
        new WorkspaceManager(globalContext, workspaceContext)
            .listResources().stream()
                .filter(
                    (resource) -> {
                      boolean stewardshipMatches =
                          stewardship == null
                              || resource.getMetadata().getStewardshipType().equals(stewardship);
                      boolean typeMatches =
                          type == null || resource.getMetadata().getResourceType().equals(type);
                      return stewardshipMatches && typeMatches;
                    })
                .collect(Collectors.toList());

    formatOption.printReturnValue(resources, List::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<ResourceDescription> returnValue) {
    for (ResourceDescription resource : returnValue) {
      ResourceMetadata metadata = resource.getMetadata();
      OUT.println(
          metadata.getName()
              + " ("
              + metadata.getResourceType()
              + ", "
              + metadata.getStewardshipType()
              + "): "
              + metadata.getDescription());
    }
  }
}
