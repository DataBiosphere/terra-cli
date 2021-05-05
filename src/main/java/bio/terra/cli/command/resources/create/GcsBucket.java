package bio.terra.cli.command.resources.create;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.CreateControlledResource;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.resources.GcsBucketLifecycle;
import bio.terra.cli.context.utils.FileUtils;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ControlledResourceMetadata;
import bio.terra.workspace.model.GcpGcsBucketAttributes;
import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import bio.terra.workspace.model.PrivateResourceIamRoles;
import bio.terra.workspace.model.PrivateResourceUser;
import bio.terra.workspace.model.ResourceAttributesUnion;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceMetadata;
import com.fasterxml.jackson.databind.MapperFeature;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources create gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Add a controlled GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin CreateControlledResource createControlledResourceOptions;

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

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
  GcsBucket.LifecycleArgGroup lifecycleArgGroup;

  static class LifecycleArgGroup {
    @CommandLine.Option(
        names = "--lifecycle",
        description =
            "Lifecycle rules (https://cloud.google.com/storage/docs/lifecycle) specified in a JSON-formatted file. See the README for the expected JSON format.")
    private Path pathToLifecycleFile;

    @CommandLine.Option(
        names = "--auto-delete",
        description =
            "Number of days after which to auto-delete the objects in the bucket. This option is a shortcut for specifying a lifecycle rule that auto-deletes objects in the bucket after some number of days.")
    private Integer autoDelete;
  }

  @CommandLine.Mixin Format formatOption;

  /** Add a controlled GCS bucket to the workspace. */
  @Override
  protected void execute() {
    createControlledResourceOptions.validateAccessOptions();

    // build the lifecycle object
    GcsBucketLifecycle lifecycle;
    if (lifecycleArgGroup == null) {
      // empty lifecycle rule object
      lifecycle = new GcsBucketLifecycle();
    } else if (lifecycleArgGroup.autoDelete != null) {
      // build an auto-delete lifecycle rule and set the number of days
      lifecycle = GcsBucketLifecycle.buildAutoDeleteRule(lifecycleArgGroup.autoDelete);
    } else {
      // read in the lifecycle rules from a file
      try {
        lifecycle =
            FileUtils.readFileIntoJavaObject(
                lifecycleArgGroup.pathToLifecycleFile.toFile(),
                GcsBucketLifecycle.class,
                Collections.singletonList(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS));
      } catch (IOException ioEx) {
        throw new UserActionableException("Error reading lifecycle rules from file.", ioEx);
      }
    }

    // build the resource object to create
    PrivateResourceIamRoles privateResourceIamRoles = new PrivateResourceIamRoles();
    if (createControlledResourceOptions.privateIamRoles != null
        && !createControlledResourceOptions.privateIamRoles.isEmpty()) {
      privateResourceIamRoles.addAll(createControlledResourceOptions.privateIamRoles);
    }
    ResourceDescription resourceToCreate =
        new ResourceDescription()
            .metadata(
                new ResourceMetadata()
                    .name(createControlledResourceOptions.name)
                    .description(createControlledResourceOptions.description)
                    .cloningInstructions(createControlledResourceOptions.cloning)
                    .controlledResourceMetadata(
                        new ControlledResourceMetadata()
                            .accessScope(createControlledResourceOptions.access)
                            .privateResourceUser(
                                new PrivateResourceUser()
                                    .userName(createControlledResourceOptions.privateUserEmail)
                                    .privateResourceIamRoles(privateResourceIamRoles))))
            .resourceAttributes(
                new ResourceAttributesUnion()
                    .gcpGcsBucket(new GcpGcsBucketAttributes().bucketName(bucketName)));

    ResourceDescription resourceCreated =
        new WorkspaceManager(globalContext, workspaceContext)
            .createControlledGcsBucket(resourceToCreate, storageClass, lifecycle, location);
    formatOption.printReturnValue(resourceCreated, GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added controlled GCS bucket.");
    PrintingUtils.printText(returnValue);
  }
}
