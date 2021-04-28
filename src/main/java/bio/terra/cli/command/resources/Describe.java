package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources describe" command. */
@CommandLine.Command(name = "describe", description = "Describe a resource.")
public class Describe extends BaseCommand {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "Name of the resource, scoped to the workspace.")
  private String name;

  @CommandLine.Mixin FormatOption formatOption;

  /** Describe a resource. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(name);
    formatOption.printReturnValue(resource, Describe::printText);
  }

  /** Print this command's output in text format. */
  public static void printText(ResourceDescription returnValue) {
    OUT.println("Name:         " + returnValue.getMetadata().getName());
    OUT.println("Description:  " + returnValue.getMetadata().getDescription());
    OUT.println("Stewardship:  " + returnValue.getMetadata().getStewardshipType());

    if (returnValue.getMetadata().getControlledResourceMetadata() != null) {
      OUT.println(
          "Access scope: "
              + returnValue.getMetadata().getControlledResourceMetadata().getAccessScope());
      OUT.println(
          "Managed by:   "
              + returnValue.getMetadata().getControlledResourceMetadata().getManagedBy());
      if (returnValue
              .getMetadata()
              .getControlledResourceMetadata()
              .getPrivateResourceUser()
              .getUserName()
          != null) {
        OUT.println(
            "Private user: "
                + returnValue
                    .getMetadata()
                    .getControlledResourceMetadata()
                    .getPrivateResourceUser()
                    .getUserName());
      }
    }

    switch (returnValue.getMetadata().getResourceType()) {
      case BIG_QUERY_DATASET:
        OUT.println(
            "GCP project id:       "
                + returnValue.getResourceAttributes().getGcpBqDataset().getProjectId());
        OUT.println(
            "Big Query dataset id: "
                + returnValue.getResourceAttributes().getGcpBqDataset().getDatasetId());
        break;
      case GCS_BUCKET:
        OUT.println(
            "GCS bucket name: "
                + returnValue.getResourceAttributes().getGcpGcsBucket().getBucketName());
        break;
      default:
    }
  }
}
