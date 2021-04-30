package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.CreateControlledResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ControlledResourceMetadata;
import bio.terra.workspace.model.GcpGcsBucketAttributes;
import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import bio.terra.workspace.model.PrivateResourceIamRoles;
import bio.terra.workspace.model.PrivateResourceUser;
import bio.terra.workspace.model.ResourceAttributesUnion;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import java.util.Collections;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources create gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Add a controlled GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin CreateControlledResource createControlledResourceMixin;

  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket')")
  private String bucketName;

  @CommandLine.Option(
      names = "--storage",
      description =
          "Storage class (https://cloud.google.com/storage/docs/storage-classes): ${COMPLETION-CANDIDATES}")
  private GcpGcsBucketDefaultStorageClass storageClass;

  @CommandLine.Option(
      names = "--location",
      description = "Bucket location (https://cloud.google.com/storage/docs/locations)")
  private String location;

  @CommandLine.Mixin Format formatOption;

  /** Add a controlled GCS bucket to the workspace. */
  @Override
  protected void execute() {
    createControlledResourceMixin.validateAccessOptions();

    // build the resource object to create
    PrivateResourceIamRoles privateResourceIamRoles = new PrivateResourceIamRoles();
    if (createControlledResourceMixin.privateIamRoles != null
        && !createControlledResourceMixin.privateIamRoles.isEmpty()) {
      privateResourceIamRoles.addAll(createControlledResourceMixin.privateIamRoles);
    }
    ResourceDescription resourceToCreate =
        new ResourceDescription()
            .metadata(
                new ResourceMetadata()
                    .name(createControlledResourceMixin.name)
                    .description(createControlledResourceMixin.description)
                    .cloningInstructions(createControlledResourceMixin.cloning)
                    .controlledResourceMetadata(
                        new ControlledResourceMetadata()
                            .accessScope(createControlledResourceMixin.access)
                            .privateResourceUser(
                                new PrivateResourceUser()
                                    .userName(createControlledResourceMixin.privateUserEmail)
                                    .privateResourceIamRoles(privateResourceIamRoles))))
            .resourceAttributes(
                new ResourceAttributesUnion()
                    .gcpGcsBucket(new GcpGcsBucketAttributes().bucketName(bucketName)));

    // TODO (PF-486): allow the user to specify lifecycle rules on the bucket
    ResourceDescription resourceCreated =
        new WorkspaceManager(globalContext, workspaceContext)
            .createControlledGcsBucket(
                resourceToCreate, storageClass, Collections.emptyList(), location);
    formatOption.printReturnValue(resourceCreated, GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added controlled GCS bucket.");
    PrintingUtils.printResource(returnValue);
  }
}
