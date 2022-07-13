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
          "Name of the resource in the workspace or path to the resource in the data source in the "
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
    String[] splits = resourceName.split("/");
    if (splits.length > 2) {
      throw new UserActionableException(
          String.format(
              "Invalid path provided: %s, only support resolving [resource name] or "
                  + "[data source name]/[resource name].",
              resourceName));
    }

    Resource resource = Context.requireWorkspace().getResource(splits[0]);

    JSONObject resourceNamesToCloudIds = new JSONObject();
    switch (resource.getResourceType()) {
      case GCS_BUCKET:
        resourceNamesToCloudIds.put(
            resource.getName(), ((GcsBucket) resource).resolve(!excludeBucketPrefix));
        break;
      case GCS_OBJECT:
        resourceNamesToCloudIds.put(
            resource.getName(), ((GcsObject) resource).resolve(!excludeBucketPrefix));
        break;
      case BQ_DATASET:
        resourceNamesToCloudIds.put(
            resource.getName(), ((BqDataset) resource).resolve(bqPathFormat));
        break;
      case BQ_TABLE:
        resourceNamesToCloudIds.put(resource.getName(), ((BqTable) resource).resolve(bqPathFormat));
        break;
      case DATA_SOURCE:
        if (splits.length == 2) {
          resourceNamesToCloudIds.put(splits[1], ((DataSource) resource).resolve(splits[1]));
        } else {
          // Put the cloudId of all the resources in the data source to resourceNamesToCloudIds.
          ((DataSource) resource)
              .getDataSourceWorkspace().getResources().stream()
                  // There shouldn't be any data source resources in a data source workspace, but
                  // filter
                  // out just in case
                  .filter(r -> r.getResourceType() != Resource.Type.DATA_SOURCE)
                  .forEach(r -> resourceNamesToCloudIds.put(r.getName(), r.resolve()));
        }
        break;
      default:
        resourceNamesToCloudIds.put(resource.getName(), resource.resolve());
    }
    formatOption.printReturnValue(resourceNamesToCloudIds, this::printText, this::printJson);
  }

  private void printText(JSONObject resourceNameToCloudId) {
    // No need to print the resource name as well as the cloudId if there's only one resource.
    boolean printResourceName = resourceNameToCloudId.length() > 1;
    for (var resourceName : resourceNameToCloudId.keySet()) {
      if (printResourceName) {
        OUT.println(resourceName + ": " + resourceNameToCloudId.get((String) resourceName));
      } else {
        OUT.println(resourceNameToCloudId.get((String) resourceName));
      }
    }
  }

  private void printJson(JSONObject resourceNameToCloudId) {
    // "2" prevents entire dict from being printed on one line and to stay consistent with the rest
    // of JSON formatted output.
    OUT.println(resourceNameToCloudId.toString(2));
  }
}
