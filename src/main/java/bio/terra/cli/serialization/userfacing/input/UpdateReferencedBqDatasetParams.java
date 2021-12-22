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
  private final UpdateResourceParams resourceParams;
  private final @Nullable String datasetId;
  private final @Nullable String projectId;
  private final String originalDatasetId;
  private final String originalProjectId;

  protected UpdateReferencedBqDatasetParams(UpdateReferencedBqDatasetParams.Builder builder) {
    this.resourceParams = builder.resourceFields;
    this.datasetId = builder.datasetId;
    this.projectId = builder.projectId;
    this.originalDatasetId = builder.originalDatasetId;
    this.originalProjectId = builder.originalProjectId;
  }

  public UpdateResourceParams getResourceParams() {
    return resourceParams;
  }

  /**
   * Gets the datasetId to be updated to.
   *
   * <p>When {@code datasetId} is null, do not update the tableId. Instead, use the datasetId from
   * the {@code originalResource}.
   */
  public @Nullable String getDatasetId() {
    return datasetId;
  }

  /**
   * Gets the projectId to be updated to.
   *
   * <p>When {@code projectId} is null, do not update the tableId. Instead, use the projectId from
   * the {@code originalResource}.
   */
  public @Nullable String getProjectId() {
    return projectId;
  }

  /** Gets the original BqDataset id that the reference is pointing to. */
  public String getOriginalDatasetId() {
    return originalDatasetId;
  }

  /** Gets the original BqDataset project id that the reference is pointing to. */
  public String getOriginalProjectId() {
    return originalProjectId;
  }

  /** Whether to update the reference to point to another BqDataset. */
  public boolean hasNewReferenceTargetFields() {
    return datasetId != null || projectId != null;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private UpdateResourceParams resourceFields;
    private String datasetId;
    private String projectId;
    private String originalDatasetId;
    private String originalProjectId;

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

    public UpdateReferencedBqDatasetParams.Builder originalDatasetId(String originalDatasetId) {
      this.originalDatasetId = originalDatasetId;
      return this;
    }

    public UpdateReferencedBqDatasetParams.Builder originalProjectId(String originalProjectId) {
      this.originalProjectId = originalProjectId;
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
