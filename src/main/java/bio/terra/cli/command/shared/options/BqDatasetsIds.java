package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the project id and dataset id when building a BigQuery datasets
 * or BigQuery data table.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class BqDatasetsIds {
  @CommandLine.Option(names = "--project-id", description = "GCP project id of the dataset.")
  private String gcpProjectId;

  @CommandLine.Option(names = "--dataset-id", description = "BigQuery dataset id.")
  private String bigQueryDatasetId;

  public String getGcpProjectId() {
    return gcpProjectId;
  }

  public String getBigQueryDatasetId() {
    return bigQueryDatasetId;
  }
}
