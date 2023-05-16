package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.AwsS3StorageFolder;
import bio.terra.cli.businessobject.resource.BqDataset;
import bio.terra.cli.businessobject.resource.BqResolvedOptions;
import bio.terra.cli.businessobject.resource.BqTable;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import org.json.JSONObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resource resolve" command. */
@Command(name = "resolve", description = "Resolve a resource to its cloud id or path.")
public class Resolve extends WsmBaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Option(
      names = "--exclude-bucket-prefix",
      description =
          "[For GCS_BUCKET and GCS_OBJECT] Exclude the 'gs://' prefix, "
              + "[For AWS_S3_STORAGE_FOLDER] Exclude the 's3://' prefix.")
  private boolean excludeBucketPrefix;

  @CommandLine.Option(
      names = "--bq-path",
      showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
      description =
          "[For BIG_QUERY_DATASET and BIG_QUERY_DATA_TABLE] Cloud id format: FULL_PATH=[project id].[dataset id].[table id if applicable], "
              + "DATASET_ID_ONLY=[dataset id], PROJECT_ID_ONLY=[project id], "
              + "[For BIG_QUERY_DATA_TABLE only] TABLE_ID_ONLY=[data table id]")
  private final BqResolvedOptions bqPathFormat = BqResolvedOptions.FULL_PATH;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Resolve a resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);

    String cloudId =
        switch (resource.getResourceType()) {
          case GCS_BUCKET -> ((GcsBucket) resource).resolve(!excludeBucketPrefix);
          case GCS_OBJECT -> ((GcsObject) resource).resolve(!excludeBucketPrefix);
          case BQ_DATASET -> ((BqDataset) resource).resolve(bqPathFormat);
          case BQ_TABLE -> ((BqTable) resource).resolve(bqPathFormat);
          case AWS_S3_STORAGE_FOLDER -> ((AwsS3StorageFolder) resource)
              .resolve(!excludeBucketPrefix);
          default -> resource.resolve();
        };
    JSONObject object = new JSONObject();
    object.put(resource.getName(), cloudId);
    formatOption.printReturnValue(object, this::printText);
  }

  private void printText(JSONObject object) {
    OUT.println(object.get(resourceNameOption.name));
  }
}
