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
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import com.google.common.base.Preconditions;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resource resolve" command. */
@Command(name = "resolve", description = "Resolve a resource to its cloud id or path.")
public class Resolve extends BaseCommand {

  @CommandLine.Mixin ResourceName resourceNameOption;

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
    String[] paths = resourceNameOption.name.split("/");
    Preconditions.checkArgument(paths.length == 1 || paths.length == 2);

    Resource resource = Context.requireWorkspace().getResource(paths[0]);


    JSONArray cloudIds = new JSONArray();
    switch (resource.getResourceType()) {
      case GCS_BUCKET:
        cloudIds.put(((GcsBucket) resource).resolve(!excludeBucketPrefix));
        break;
      case GCS_OBJECT:
        cloudIds.put(((GcsObject) resource).resolve(!excludeBucketPrefix));
        break;
      case BQ_DATASET:
        cloudIds.put(((BqDataset) resource).resolve(bqPathFormat));
        break;
      case BQ_TABLE:
        cloudIds.put(((BqTable) resource).resolve(bqPathFormat));
        break;
      case DATA_SOURCE:
        if (paths.length == 2) {
          cloudIds.put(((DataSource) resource).resolve(paths[1]));
        } else {
          var resourcesOptional = ((DataSource) resource).getResources();
          if (resourcesOptional.isPresent()) {
            cloudIds.put(resourcesOptional.map(
                resources ->
                    resources.stream()
                        .filter(r -> r.getResourceType() != Resource.Type.DATA_SOURCE)
                        .map(Resource::resolve)
                        .collect(Collectors.toList())
            ).orElse(Collections.emptyList()));
          }
        }
        break;
      default:
        cloudIds.put(resource.resolve());
    }
    formatOption.printReturnValue(cloudIds, this::printText, this::printJson);
  }

  private void printText(JSONArray arr) {
    OUT.println(arr.join("\n"));
  }

  private void printJson(JSONArray arr) {
    OUT.println(arr);
  }
}
