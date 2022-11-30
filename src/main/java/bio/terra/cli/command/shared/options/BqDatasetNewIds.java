package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the project id and dataset id when updating a BigQuery datasets
 * or BigQuery data table.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class BqDatasetNewIds {
  @CommandLine.Option(
      names = "--new-project-id",
      description = "New GCP project id of the dataset.")
  private String newGcpProjectId;

  @CommandLine.Option(names = "--new-dataset-id", description = "New BigQuery dataset id.")
  private String newBqDatasetId;

  public String getNewGcpProjectId() {
    return newGcpProjectId;
  }

  public String getNewBqDatasetId() {
    return newBqDatasetId;
  }

  public boolean isDefined() {
    return newGcpProjectId != null || newBqDatasetId != null;
  }
}
