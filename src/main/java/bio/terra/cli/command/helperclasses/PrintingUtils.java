package bio.terra.cli.command.helperclasses;

import static bio.terra.cli.command.helperclasses.BaseCommand.OUT;

import bio.terra.cli.context.WorkspaceContext;
import bio.terra.workspace.model.GcpAiNotebookInstanceAttributes;
import bio.terra.workspace.model.ResourceDescription;

/** Utility methods for printing command output. */
public class PrintingUtils {

  /** Print out a workspace object in text format. */
  public static void printText(WorkspaceContext workspaceContext) {
    OUT.println("Terra workspace id: " + workspaceContext.getWorkspaceId());
    OUT.println("Display name: " + workspaceContext.getWorkspaceDisplayName().orElse(""));
    OUT.println("Description: " + workspaceContext.getWorkspaceDescription().orElse(""));
    OUT.println("Google project: " + workspaceContext.getGoogleProject());
  }

  /** Print out a resource object in text format. */
  public static void printText(ResourceDescription resource) {
    OUT.println("Name:         " + resource.getMetadata().getName());
    OUT.println("Description:  " + resource.getMetadata().getDescription());
    OUT.println("Stewardship:  " + resource.getMetadata().getStewardshipType());

    if (resource.getMetadata().getControlledResourceMetadata() != null) {
      OUT.println(
          "Access scope: "
              + resource.getMetadata().getControlledResourceMetadata().getAccessScope());
      OUT.println(
          "Managed by:   " + resource.getMetadata().getControlledResourceMetadata().getManagedBy());
      if (resource
              .getMetadata()
              .getControlledResourceMetadata()
              .getPrivateResourceUser()
              .getUserName()
          != null) {
        OUT.println(
            "Private user: "
                + resource
                    .getMetadata()
                    .getControlledResourceMetadata()
                    .getPrivateResourceUser()
                    .getUserName());
      }
    }

    switch (resource.getMetadata().getResourceType()) {
      case BIG_QUERY_DATASET:
        OUT.println(
            "GCP project id:       "
                + resource.getResourceAttributes().getGcpBqDataset().getProjectId());
        OUT.println(
            "Big Query dataset id: "
                + resource.getResourceAttributes().getGcpBqDataset().getDatasetId());
        break;
      case GCS_BUCKET:
        OUT.println(
            "GCS bucket name: "
                + resource.getResourceAttributes().getGcpGcsBucket().getBucketName());
        break;
      case AI_NOTEBOOK:
        // TODO(PF-729): Consider how to print more resource specific information, such as the
        // current state/proxy URL found in `terra notebooks get`.
        GcpAiNotebookInstanceAttributes notebookAttributes =
            resource.getResourceAttributes().getGcpAiNotebookInstance();
        OUT.println("GCP project id:                " + notebookAttributes.getProjectId());
        OUT.println("AI Notebook instance location: " + notebookAttributes.getLocation());
        OUT.println("AI Notebook instance id:       " + notebookAttributes.getInstanceId());
        break;
      default:
    }
  }
}
