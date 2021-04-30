package bio.terra.cli.command.resources.addref;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.CreateResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.GcpGcsBucketAttributes;
import bio.terra.workspace.model.ResourceAttributesUnion;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources add-ref gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Add a referenced GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin CreateResource createResourceOptions;

  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket')")
  private String bucketName;

  @CommandLine.Mixin Format formatOption;

  /** Add a referenced GCS bucket to the workspace. */
  @Override
  protected void execute() {
    // build the resource object to add
    ResourceDescription resourceToAdd =
        new ResourceDescription()
            .metadata(
                new ResourceMetadata()
                    .name(createResourceOptions.name)
                    .description(createResourceOptions.description)
                    .cloningInstructions(createResourceOptions.cloning))
            .resourceAttributes(
                new ResourceAttributesUnion()
                    .gcpGcsBucket(new GcpGcsBucketAttributes().bucketName(bucketName)));

    ResourceDescription resourceAdded =
        new WorkspaceManager(globalContext, workspaceContext)
            .createReferencedGcsBucket(resourceToAdd);
    formatOption.printReturnValue(resourceAdded, GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added referenced GCS bucket.");
    PrintingUtils.printText(returnValue);
  }
}
