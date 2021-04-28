package bio.terra.cli.command.resources.addref;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources add-ref gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Add a referenced GCS bucket.",
    showDefaultValues = true)
public class GcsBucket extends BaseCommand {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description =
          "Name of the resource, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
  private String name;

  @CommandLine.Option(names = "--description", description = "Description of the resource")
  private String description;

  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}")
  private CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;

  @CommandLine.Option(
      names = "--bucket-name",
      required = true,
      description =
          "Name of the GCS bucket, without the prefix. (e.g. 'my-bucket', not 'gs://my-bucket')")
  private String bucketName;

  @CommandLine.Mixin FormatOption formatOption;

  /** Add a referenced GCS bucket to the workspace. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext)
            .createReferencedGcsBucket(name, description, cloning, bucketName);
    formatOption.printReturnValue(resource, GcsBucket::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(ResourceDescription returnValue) {
    OUT.println("Successfully added referenced GCS bucket.");
    bio.terra.cli.command.resources.Describe.printText(returnValue);
  }
}
