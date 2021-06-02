package bio.terra.cli.serialization.disk.resources;

import bio.terra.cli.resources.BqDataset;
import bio.terra.cli.serialization.disk.DiskResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = DiskBqDataset.Builder.class)
public class DiskBqDataset extends DiskResource {
  public final String projectId;
  public final String datasetId;

  private DiskBqDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends DiskResource.Builder {
    private String projectId;
    private String datasetId;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    /** Call the private constructor. */
    public DiskBqDataset build() {
      return new DiskBqDataset(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the disk format. */
    public Builder(BqDataset internalObj) {
      super(internalObj);
      this.projectId = internalObj.getProjectId();
      this.datasetId = internalObj.getDatasetId();
    }
  }
}
