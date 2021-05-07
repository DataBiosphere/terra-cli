package bio.terra.cli.command.resources;

import static bio.terra.cli.service.WorkspaceManager.getBigQueryDatasetPath;
import static bio.terra.cli.service.WorkspaceManager.getGcsBucketUrl;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.ResourceName;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resources resolve" command. */
@Command(name = "resolve", description = "Resolve a resource to its cloud id or path.")
public class Resolve extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Option(
      names = "--exclude-bucket-prefix",
      description = "[For GCS_BUCKET] Exclude the 'gs://' prefix.")
  private boolean excludeBucketPrefix;

  @CommandLine.Option(
      names = "--bq-path",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
      description =
          "[For BIG_QUERY_DATASET] Cloud id format: FULL_PATH=[project id].[dataset id], DATASET_ID_ONLY=[dataset id], PROJECT_ID_ONLY=[project id]")
  private BqDatasetResolveOptions bqPathFormat = BqDatasetResolveOptions.FULL_PATH;

  @CommandLine.Mixin Format formatOption;

  /** Resolve a resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(resourceNameOption.name);

    // the cloud identifier is resource-type specific
    String cloudId;
    switch (resource.getMetadata().getResourceType()) {
      case GCS_BUCKET:
        cloudId =
            excludeBucketPrefix
                ? resource.getResourceAttributes().getGcpGcsBucket().getBucketName()
                : getGcsBucketUrl(resource);
        break;
      case BIG_QUERY_DATASET:
        cloudId = bqPathFormat.toCloudId(resource);
        break;
      default:
        throw new UnsupportedOperationException(
            "Resource type not supported: " + resource.getMetadata().getResourceType());
    }
    formatOption.printReturnValue(cloudId);
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
