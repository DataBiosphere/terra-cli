package bio.terra.cli.serialization.userfacing.input.referenced;

import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a BigQuery table workspace referenced resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedBqTableParams {
  private final UpdateReferencedBqDatasetParams updateReferencedBqDatasetParams;
  private final String tableId;

  protected UpdateReferencedBqTableParams(UpdateReferencedBqTableParams.Builder builder) {
    this.updateReferencedBqDatasetParams = builder.updateReferencedBqDatasetParams;
    this.tableId = builder.tableId;
  }

  public UpdateReferencedBqDatasetParams getUpdateReferencedBqDatasetParams() {
    return updateReferencedBqDatasetParams;
  }

  public String getTableId() {
    return tableId;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private UpdateReferencedBqDatasetParams updateReferencedBqDatasetParams;
    private String tableId;

    public UpdateReferencedBqTableParams.Builder updateReferencedBqDatasetParams(
        UpdateReferencedBqDatasetParams updateReferencedBqDatasetParams) {
      this.updateReferencedBqDatasetParams = updateReferencedBqDatasetParams;
      return this;
    }

    public UpdateReferencedBqTableParams.Builder tableId(String tableId) {
      this.tableId = tableId;
      return this;
    }

    /** Call the private constructor. */
    public UpdateReferencedBqTableParams build() {
      return new UpdateReferencedBqTableParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
