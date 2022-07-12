package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.BqDataset;
import bio.terra.cli.businessobject.resource.BqResolvedOptions;
import bio.terra.cli.businessobject.resource.BqTable;
import bio.terra.cli.businessobject.resource.DataSource;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import org.json.JSONObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resource resolve" command. */
@Command(name = "resolve", description = "Resolve a resource to its cloud id or path.")
public class Resolve extends BaseCommand {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description =
          "Name of the resource in the workspace or path to the resource in the data source in the"
              + "format of [data source name]/[resource name]")
  public String resourceName;

  @CommandLine.Option(
      names = "--exclude-bucket-prefix",
      description = "[For GCS_BUCKET and GCS_OBJECT] Exclude the 'gs://' prefix.")
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
    String[] paths = resourceName.split("/");
    if (paths.length > 2) {
      throw new UserActionableException(
          String.format(
              "Invalid path provided: %s, only support resolving [resource name] or"
                  + "[data source name]/[resource name]",
              resourceName));
    }

    Resource resource = Context.requireWorkspace().getResource(paths[0]);

    JSONObject cloudIds = new JSONObject();
    switch (resource.getResourceType()) {
      case GCS_BUCKET:
        cloudIds.put(resource.getName(), ((GcsBucket) resource).resolve(!excludeBucketPrefix));
        break;
      case GCS_OBJECT:
        cloudIds.put(resource.getName(), ((GcsObject) resource).resolve(!excludeBucketPrefix));
        break;
      case BQ_DATASET:
        cloudIds.put(resource.getName(), ((BqDataset) resource).resolve(bqPathFormat));
        break;
      case BQ_TABLE:
        cloudIds.put(resource.getName(), ((BqTable) resource).resolve(bqPathFormat));
        break;
      case DATA_SOURCE:
        if (paths.length == 2) {
          cloudIds.put(paths[1], ((DataSource) resource).resolve(paths[1]));
        } else {
          var resources = ((DataSource) resource).getDataSourceWorkspace().getResources();
          resources.stream()
              .filter(r -> r.getResourceType() != Resource.Type.DATA_SOURCE)
              .forEach(r -> cloudIds.put(r.getName(), r.resolve()));
        }
        break;
      default:
        cloudIds.put(resource.getName(), resource.resolve());
    }
    formatOption.printReturnValue(cloudIds, this::printText, this::printJson);
  }

  private void printText(JSONObject object) {
    for (var key : object.keySet()) {
      OUT.println(key + ": " + object.get((String) key));
    }
  }

  private void printJson(JSONObject object) {
    OUT.println(object.toString(2));
  }
}
