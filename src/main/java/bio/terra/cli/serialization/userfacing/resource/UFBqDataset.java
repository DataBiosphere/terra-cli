package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.BqDataset;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

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
  public final int tableCount;

  /** Serialize an instance of the internal class to the command format. */
  public UFBqDataset(BqDataset internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.datasetId = internalObj.getDatasetId();
    this.tableCount = internalObj.getTableCount();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFBqDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.tableCount = builder.tableCount;
  }

  /** Print out this object in text format. */
  @Override
  public void print() {
    super.print();
    PrintStream OUT = UserIO.getOut();
    OUT.println("GCP project id: " + projectId);
    OUT.println("BigQuery dataset id: " + datasetId);
    OUT.println("Table count: " + tableCount);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String projectId;
    private String datasetId;
    private int tableCount;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public Builder tableCount(int tableCount) {
      this.tableCount = tableCount;
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
