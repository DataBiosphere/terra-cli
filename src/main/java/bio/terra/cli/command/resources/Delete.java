package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources delete" command. */
@CommandLine.Command(name = "delete", description = "Delete a resource from the workspace.")
public class Delete extends BaseCommand {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "Name of the resource, scoped to the workspace.")
  private String name;

  @CommandLine.Mixin FormatOption formatOption;

  /** Delete a resource from the workspace. */
  @Override
  protected void execute() {
    // get the resource summary object
    WorkspaceManager workspaceManager = new WorkspaceManager(globalContext, workspaceContext);
    ResourceDescription resourceToDelete = workspaceManager.getResource(name);

    // call the appropriate delete endpoint for the resource
    // there is a different endpoint(s) for deleting each combination of resource type and
    // stewardship type, but all require only the workspace and resource unique ids, so calling them
    // looks very similar from the CLI user's perspective
    if (resourceToDelete.getMetadata().getStewardshipType().equals(StewardshipType.REFERENCED)) {
      switch (resourceToDelete.getMetadata().getResourceType()) {
        case GCS_BUCKET:
          workspaceManager.deleteReferencedGcsBucket(name);
          break;
        case BIG_QUERY_DATASET:
          workspaceManager.deleteReferencedBigQueryDataset(name);
          break;
        default:
          throw new UnsupportedOperationException("Other resource types are not available, yet.");
      }
    } else if (resourceToDelete
        .getMetadata()
        .getStewardshipType()
        .equals(StewardshipType.CONTROLLED)) {
      switch (resourceToDelete.getMetadata().getResourceType()) {
        case GCS_BUCKET:
          workspaceManager.deleteControlledGcsBucket(name);
          break;
        case BIG_QUERY_DATASET:
          workspaceManager.deleteControlledBigQueryDataset(name);
          break;
        default:
          throw new UnsupportedOperationException("Other resource types are not available, yet.");
      }
    } else {
      throw new UnsupportedOperationException("Other stewardship types are not supported.");
    }

    formatOption.printReturnValue(resourceToDelete, Delete::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully deleted resource.");
    bio.terra.cli.command.resources.Describe.printText(returnValue);
  }
}
