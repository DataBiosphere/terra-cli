package bio.terra.cli.command;

import static bio.terra.cli.service.WorkspaceManager.getBigQueryDatasetPath;
import static bio.terra.cli.service.WorkspaceManager.getGcsBucketUrl;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra resolve" command. */
@Command(name = "resolve", description = "Resolve a resource to its cloud id or path.")
public class Resolve extends BaseCommand {
  @CommandLine.Parameters(
      index = "0",
      description = "Name of the resource, scoped to the workspace.")
  private String resourceName;

  @CommandLine.Mixin Format formatOption;

  /** Resolve a resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(resourceName);

    // return the "default" cloud identifier (i.e. the format returned by the `terra resource
    // resolve ...` commands if no options are specified)
    String cloudId;
    switch (resource.getMetadata().getResourceType()) {
      case GCS_BUCKET:
        cloudId = getGcsBucketUrl(resource);
        break;
      case BIG_QUERY_DATASET:
        cloudId = getBigQueryDatasetPath(resource);
        break;
      default:
        throw new UnsupportedOperationException(
            "Resource type not supported: " + resource.getMetadata().getResourceType());
    }
    formatOption.printReturnValue(cloudId);
  }
}
