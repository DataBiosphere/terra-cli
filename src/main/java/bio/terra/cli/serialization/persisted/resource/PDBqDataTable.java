package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.BqDataTable;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace BQ data table resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link BqDataTable} class for a data table's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDBqDataTable.Builder.class)
public class PDBqDataTable extends PDResource {
  public final String projectId;
  public final String datasetId;
  public final String dataTableId;

  /** Serialize an instance of the internal class to the disk format. */
  public PDBqDataTable(BqDataTable internalObj) {
    super(internalObj);
    this.projectId = internalObj.getProjectId();
    this.datasetId = internalObj.getDatasetId();
    this.dataTableId = internalObj.getDataTableId();
  }

  private PDBqDataTable(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.dataTableId = builder.dataTableId;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public BqDataTable deserializeToInternal() {
    return new BqDataTable(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String projectId;
    private String datasetId;
    private String dataTableId;

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
    public PDBqDataTable build() {
      return new PDBqDataTable(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
