package bio.terra.cli.command.resource;

import bio.terra.cli.app.utils.tables.ColumnDefinition;
import bio.terra.cli.app.utils.tables.TablePrinter;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.resource.BqDataset;
import bio.terra.cli.businessobject.resource.BqResolvedOptions;
import bio.terra.cli.businessobject.resource.BqTable;
import bio.terra.cli.businessobject.resource.DataCollection;
import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import java.util.ArrayList;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
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
          "Name of the resource in the workspace or path to the resource in the data collection in the "
              + "format of [data collection name]/[resource name]")
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
                  + "[data collection name]/[resource name].",
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
      case DATA_COLLECTION:
        if (splits.length == 2) {
          resourceNamesToCloudIds.put(splits[1], ((DataCollection) resource).resolve(splits[1]));
        } else {
          // Put the cloudId of all the resources in the data collection to resourceNamesToCloudIds.
          ((DataCollection) resource)
              .getDataCollectionWorkspace().getResources().stream()
                  // There shouldn't be any data collection resources in a data collection
                  // workspace,
                  // but filter out just in case
                  .filter(r -> r.getResourceType() != Resource.Type.DATA_COLLECTION)
                  .forEach(r -> resourceNamesToCloudIds.put(r.getName(), r.resolve()));
        }
        break;
      default:
        resourceNamesToCloudIds.put(resource.getName(), resource.resolve());
    }
    formatOption.printReturnValue(resourceNamesToCloudIds, this::printText, this::printJson);
  }

  private void printText(JSONObject resourceNamesToCloudIds) {
    // For a single resource, just print cloud ID (no resource name)
    if (resourceNamesToCloudIds.length() == 1) {
      String resourceName = (String) resourceNamesToCloudIds.keySet().iterator().next();
      OUT.println(resourceNamesToCloudIds.get(resourceName));
    }

    // These are the resources for a data collection. Print table of resource name and cloud ID.
    // Convert JSONObject to List for TablePrinter.
    java.util.List<Pair<String, String>> resourceNameToCloudIdsList = new ArrayList<>();
    resourceNamesToCloudIds
        .keySet()
        .forEach(
            resourceName ->
                resourceNameToCloudIdsList.add(
                    Pair.of(
                        (String) resourceName,
                        (String) resourceNamesToCloudIds.get((String) resourceName))));
    TablePrinter<Pair<String, String>> printer = ResolveColumns::values;
    OUT.println(printer.print(resourceNameToCloudIdsList));
  }

  private void printJson(JSONObject resourceNamesToCloudIds) {
    // "2" prevents entire dict from being printed on one line and to stay consistent with the rest
    // of JSON formatted output.
    OUT.println(resourceNamesToCloudIds.toString(2));
  }

  private enum ResolveColumns implements ColumnDefinition<Pair<String, String>> {
    NAME("NAME", Pair::getLeft, 40, Alignment.LEFT),
    CLOUD_ID("PATH", Pair::getRight, 90, Alignment.LEFT);

    private final String columnLabel;
    private final Function<Pair<String, String>, String> valueExtractor;
    private final int width;
    private final Alignment alignment;

    ResolveColumns(
        String columnLabel,
        Function<Pair<String, String>, String> valueExtractor,
        int width,
        Alignment alignment) {
      this.columnLabel = columnLabel;
      this.valueExtractor = valueExtractor;
      this.width = width;
      this.alignment = alignment;
    }

    @Override
    public String getLabel() {
      return columnLabel;
    }

    @Override
    public Function<Pair<String, String>, String> getValueExtractor() {
      return valueExtractor;
    }

    @Override
    public int getWidth() {
      return width;
    }

    @Override
    public Alignment getAlignment() {
      return alignment;
    }
  }
}
