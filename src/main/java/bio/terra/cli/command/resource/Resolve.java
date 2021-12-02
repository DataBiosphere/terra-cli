package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.BqDataset;
import bio.terra.cli.businessobject.resource.BqResolvedOptions;
import bio.terra.cli.businessobject.resource.BqTable;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resource resolve" command. */
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
          "[For BIG_QUERY_DATASET and BIG_QUERY_DATA_TABLE] Cloud id format: FULL_PATH=[project id].[dataset id].[table id if applicable], "
              + "DATASET_ID_ONLY=[dataset id], PROJECT_ID_ONLY=[project id], "
              + "[For BIG_QUERY_DATA_TABLE only] TABLE_ID_ONLY=[data table id]")
  private BqResolvedOptions bqPathFormat = BqResolvedOptions.FULL_PATH;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Resolve a resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);

    String cloudId;
    switch (resource.getResourceType()) {
      case GCS_BUCKET:
        cloudId = ((GcsBucket) resource).resolve(!excludeBucketPrefix);
        break;
      case BQ_DATASET:
        cloudId = ((BqDataset) resource).resolve(bqPathFormat);
        break;
      case BQ_TABLE:
        cloudId = ((BqTable) resource).resolve(bqPathFormat);
        break;
      default:
        cloudId = resource.resolve();
    }
    formatOption.printReturnValue(cloudId);
  }
}
