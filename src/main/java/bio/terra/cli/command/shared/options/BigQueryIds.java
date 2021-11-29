package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/** Ids for building BigQuery datasets or BigQuery data table. */
public class BigQueryIds {

  @CommandLine.Option(
      names = "--project-id",
      required = true,
      description = "GCP project id of the dataset.")
  private String gcpProjectId;

  @CommandLine.Option(names = "--dataset-id", required = true, description = "BigQuery dataset id.")
  private String bigQueryDatasetId;

  public String getGcpProjectId() {
    return gcpProjectId;
  }

  public String getBigQueryDatasetId() {
    return bigQueryDatasetId;
  }
}
