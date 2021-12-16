package bio.terra.cli.command.shared.options;

import java.util.Optional;
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
      description = "new GCP project id of the dataset.")
  private String newGcpProjectId;

  @CommandLine.Option(names = "--new-dataset-id", description = "new BigQuery dataset id.")
  private String newBqDatasetId;

  public Optional<String> getNewGcpProjectId() {
    return Optional.ofNullable(newGcpProjectId);
  }

  public Optional<String> getNewBqDatasetId() {
    return Optional.ofNullable(newBqDatasetId);
  }

  public boolean isDefined() {
    return newGcpProjectId != null || newBqDatasetId != null;
  }
}