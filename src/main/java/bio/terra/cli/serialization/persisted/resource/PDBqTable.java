package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.BqTable;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace BQ data table resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link BqTable} class for a data table's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDBqTable.Builder.class)
public class PDBqTable extends PDResource {
  public final String projectId;
  public final String datasetId;
  public final String dataTableId;

  /** Serialize an instance of the internal class to the disk format. */
  public PDBqTable(BqTable internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.datasetId = internalObj.getDatasetId();
    this.dataTableId = internalObj.getDataTableId();
  }

  private PDBqTable(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.dataTableId = builder.dataTableId;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public BqTable deserializeToInternal() {
    return new BqTable(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String projectId;
    private String datasetId;
    private String dataTableId;

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

    public Builder dataTableId(String dataTableId) {
      this.dataTableId = dataTableId;
      return this;
    }

    /** Call the private constructor. */
    public PDBqTable build() {
      return new PDBqTable(this);
    }
  }
}
