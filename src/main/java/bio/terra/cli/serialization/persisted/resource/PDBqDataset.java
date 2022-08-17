package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.BqDataset;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace BQ dataset resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link BqDataset} class for a dataset's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDBqDataset.Builder.class)
public class PDBqDataset extends PDResource {
  public final String projectId;
  public final String datasetId;

  /** Serialize an instance of the internal class to the disk format. */
  public PDBqDataset(BqDataset internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.datasetId = internalObj.getDatasetId();
  }

  private PDBqDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public BqDataset deserializeToInternal() {
    return new BqDataset(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String projectId;
    private String datasetId;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    /** Call the private constructor. */
    public PDBqDataset build() {
      return new PDBqDataset(this);
    }
  }
}
