package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.BqDataTable;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.GoogleBigQuery;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.api.services.bigquery.model.Dataset;
import java.io.PrintStream;
import java.util.Optional;

/**
 * External representation of a workspace BigQuery data table resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link bio.terra.cli.businessobject.resource.BqDataTable} class for a dataset's
 * internal representation.
 */
@JsonDeserialize(builder = UFBqDataTable.Builder.class)
public class UFBqDataTable extends UFResource {
  public final String projectId;
  public final String datasetId;
  public final String dataTableId;
  public final String location;

  /** Serialize an instance of the internal class to the command format. */
  public UFBqDataTable(BqDataTable internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.datasetId = internalObj.getDatasetId();
    this.dataTableId = internalObj.getDataTableId();

    GoogleBigQuery bigQuery = GoogleBigQuery.fromContextForPetSa();
    Optional<Dataset> dataset = bigQuery.getDataset(projectId, datasetId);
    this.location = dataset.map(Dataset::getLocation).orElse(null);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFBqDataTable(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.dataTableId = builder.dataTableId;
    this.location = builder.location;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "GCP project id: " + projectId);
    OUT.println(prefix + "BigQuery dataset id: " + datasetId + " table id: " + dataTableId);
    OUT.println(prefix + "Location: " + (location == null ? "(undefined)" : location));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String projectId;
    private String datasetId;
    private String location;
    private String dataTableId;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder dataTableId(String dataTableId) {
      this.dataTableId = dataTableId;
      return this;
    }

    /** Call the private constructor. */
    public UFBqDataTable build() {
      return new UFBqDataTable(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
