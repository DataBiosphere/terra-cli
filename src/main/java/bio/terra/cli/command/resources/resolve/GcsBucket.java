package bio.terra.cli.command.resources.resolve;

import static bio.terra.cli.service.WorkspaceManager.getGcsBucketUrl;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.ResourceName;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources resolve gcs-bucket" command. */
@CommandLine.Command(
    name = "gcs-bucket",
    description = "Resolve a GCS bucket resource to its cloud id or path.")
public class GcsBucket extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Option(names = "--exclude-prefix", description = "Exclude the 'gs://' prefix.")
  private boolean excludePrefix;

  @CommandLine.Mixin Format formatOption;

  /** Resolve a GCS bucket resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(resourceNameOption.name);
    String bucketName =
        excludePrefix
            ? resource.getResourceAttributes().getGcpGcsBucket().getBucketName()
            : getGcsBucketUrl(resource);
    formatOption.printReturnValue(bucketName);
  }
}
