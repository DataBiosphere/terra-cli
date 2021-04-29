package bio.terra.cli.command.resources.resolve;

import static bio.terra.cli.service.WorkspaceManager.getBigQueryDatasetPath;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.ResourceName;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources resolve bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Resolve a Big Query dataset resource to its cloud id or path.")
public class BqDataset extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameMixin;

  @CommandLine.Option(
      names = "--cloud-id-format",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
      description =
          "Cloud id format: FULL_PATH=[project id].[dataset id], DATASET_ID_ONLY=[dataset id], PROJECT_ID_ONLY=[project id]")
  private BqDatasetResolveOptions cloudIdFormat = BqDatasetResolveOptions.FULL_PATH;

  @CommandLine.Mixin Format formatOption;

  /** Resolve a Big Query dataset resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(resourceNameMixin.name);
    formatOption.printReturnValue(cloudIdFormat.toCloudId(resource));
  }

  /** This enum specifies the possible ways to resolve a Big Query dataset resource. */
  enum BqDatasetResolveOptions {
    FULL_PATH, // [project id].[dataset id]
    DATASET_ID_ONLY, // [dataset id]
    PROJECT_ID_ONLY; // [project id]

    /**
     * Helper method to convert a Big Query dataset resource into a cloud id. Either returns
     * [project id].[dataset id], only the project id, or only the dataset id, depending on the enum
     * value.
     */
    String toCloudId(ResourceDescription resource) {
      switch (this) {
        case FULL_PATH:
          return getBigQueryDatasetPath(resource);
        case DATASET_ID_ONLY:
          return resource.getResourceAttributes().getGcpBqDataset().getDatasetId();
        case PROJECT_ID_ONLY:
          return resource.getResourceAttributes().getGcpBqDataset().getProjectId();
        default:
          throw new IllegalArgumentException("Unknown Big Query dataset resolve operation.");
      }
    }
  }
}
