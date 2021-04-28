package bio.terra.cli.command.resources.resolve;

import static bio.terra.cli.service.WorkspaceManager.getGcsBucketUrl;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources resolve gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Resolve a GCS bucket resource to its cloud id or path.")
public class GcsBucket extends BaseCommand {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "Name of the resource, scoped to the workspace.")
  private String name;

  @CommandLine.Option(names = "--exclude-prefix", description = "Exclude the 'gs://' prefix.")
  private boolean excludePrefix;

  @CommandLine.Mixin FormatOption formatOption;

  /** Resolve a GCS bucket resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(name);
    String bucketName =
        excludePrefix
            ? resource.getResourceAttributes().getGcpGcsBucket().getBucketName()
            : getGcsBucketUrl(resource);
    formatOption.printReturnValue(bucketName);
  }
}
