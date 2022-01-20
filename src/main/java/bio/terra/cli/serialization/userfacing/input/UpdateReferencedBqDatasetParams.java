package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

/**
 * Parameters for updating a BigQuery dataset workspace referenced resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedBqDatasetParams {
  public final UpdateResourceParams resourceParams;
  /**
   * New datasetId to be updated to.
   *
   * <p>When {@code datasetId} is null, do not update the tableId. Instead, use the datasetId from
   * the {@code originalResource}.
   */
  public final @Nullable String datasetId;

  /**
   * New projectId to be updated to.
   *
   * <p>When {@code projectId} is null, do not update the tableId. Instead, use the projectId from
   * the {@code originalResource}.
   */
  public final @Nullable String projectId;

  protected UpdateReferencedBqDatasetParams(UpdateReferencedBqDatasetParams.Builder builder) {
    this.resourceParams = builder.resourceFields;
    this.datasetId = builder.datasetId;
    this.projectId = builder.projectId;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private UpdateResourceParams resourceFields;
    private String datasetId;
    private String projectId;

    public UpdateReferencedBqDatasetParams.Builder resourceParams(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateReferencedBqDatasetParams.Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public UpdateReferencedBqDatasetParams.Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    /** Call the private constructor. */
    public UpdateReferencedBqDatasetParams build() {
      return new UpdateReferencedBqDatasetParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
