package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import bio.terra.workspace.model.StewardshipType;
import java.util.stream.Collectors;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources list" command. */
@CommandLine.Command(name = "list", description = "List all resources.")
public class List extends BaseCommand {

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
  List.ListResourcesArgGroup argGroup;

  static class ListResourcesArgGroup {
    @CommandLine.Option(names = "--controlled", description = "Only list controlled resources.")
    private boolean controlled;

    @CommandLine.Option(names = "--referenced", description = "Only list referenced resources.")
    private boolean referenced;

    StewardshipType toStewardshipType() {
      if (controlled) {
        return StewardshipType.CONTROLLED;
      } else if (referenced) {
        return StewardshipType.REFERENCED;
      } else {
        // this ArgGroup is defined with exclusive=true so we should never get here
        throw new IllegalArgumentException(
            "Expected either controlled or referenced to be defined.");
      }
    }
  }

  @CommandLine.Mixin Format formatOption;

  /** List the resources in the workspace. */
  @Override
  protected void execute() {
    java.util.List<ResourceDescription> resources =
        new WorkspaceManager(globalContext, workspaceContext).listResources();

    if (argGroup != null) {
      resources =
          resources.stream()
              .filter(
                  resource ->
                      resource
                          .getMetadata()
                          .getStewardshipType()
                          .equals(argGroup.toStewardshipType()))
              .collect(Collectors.toList());
    }

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
