package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.BqDataset;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.GoogleBigQuery;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.api.services.bigquery.model.Dataset;
import java.io.PrintStream;
import java.util.Optional;

/**
 * External representation of a workspace BigQuery dataset resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link BqDataset} class for a dataset's internal representation.
 */
@JsonDeserialize(builder = UFBqDataset.Builder.class)
public class UFBqDataset extends UFResource {
  public final String projectId;
  public final String datasetId;
  public final String location;
  public final long numTables;

  /** Serialize an instance of the internal class to the command format. */
  public UFBqDataset(BqDataset internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.datasetId = internalObj.getDatasetId();

    GoogleBigQuery bigQuery = GoogleBigQuery.fromContextForPetSa();
    Optional<Dataset> dataset = bigQuery.getDataset(projectId, datasetId);
    this.location = dataset.isPresent() ? dataset.get().getLocation() : null;
    this.numTables = bigQuery.getNumTables(projectId, datasetId);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFBqDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.location = builder.location;
    this.numTables = builder.numTables;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "GCP project id: " + projectId);
    OUT.println(prefix + "BigQuery dataset id: " + datasetId);
    OUT.println(prefix + "Location: " + (location == null ? "(undefined)" : location));
    OUT.println(prefix + "# Tables: " + numTables);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String projectId;
    private String datasetId;
    private String location;
    private long numTables;

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

    public Builder numTables(long numTables) {
      this.numTables = numTables;
      return this;
    }

    /** Call the private constructor. */
    public UFBqDataset build() {
      return new UFBqDataset(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
