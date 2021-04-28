package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import bio.terra.workspace.model.ResourceDescription;
import java.util.Collections;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources create gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Add a controlled GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description =
          "Name of the resource, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
  private String name;

  @CommandLine.Option(
      names = "--description",
      required = true,
      description = "Description of the resource")
  private String description;

  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}")
  private CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;

  @CommandLine.Option(
      names = "--access",
      description = "Access scope for the resource: ${COMPLETION-CANDIDATES}")
  private AccessScope access = AccessScope.SHARED_ACCESS;

  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket')")
  private String bucketName;

  @CommandLine.Option(
      names = "--storage",
      required = true,
      description =
          "Storage class (https://cloud.google.com/storage/docs/storage-classes): ${COMPLETION-CANDIDATES}")
  private GcpGcsBucketDefaultStorageClass storageClass;

  @CommandLine.Option(
      names = "--location",
      required = true,
      description = "Bucket location (https://cloud.google.com/storage/docs/locations)")
  private String location;

  @CommandLine.Mixin FormatOption formatOption;

  /** Add a controlled GCS bucket to the workspace. */
  @Override
  protected void execute() {
    // TODO (PF-486): allow the user to specify lifecycle rules on the bucket
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext)
            .createControlledGcsBucket(
                name,
                description,
                cloning,
                access,
                bucketName,
                storageClass,
                Collections.emptyList(),
                location);
    formatOption.printReturnValue(resource, GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added controlled GCS bucket.");
    bio.terra.cli.command.resources.Describe.printText(returnValue);
  }
}
