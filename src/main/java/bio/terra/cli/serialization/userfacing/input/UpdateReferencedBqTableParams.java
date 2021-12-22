package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

/**
 * Parameters for updating a BigQuery table workspace referenced resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedBqTableParams {

  public final UpdateResourceParams resourceParams;
  private final @Nullable String datasetId;
  private final @Nullable String projectId;
  private final @Nullable String tableId;
  private final String originalDatasetId;
  private final String originalProjectId;
  private final String originalTableId;

  protected UpdateReferencedBqTableParams(UpdateReferencedBqTableParams.Builder builder) {
    this.resourceParams = builder.resourceParams;
    this.datasetId = builder.datasetId;
    this.projectId = builder.projectId;
    this.tableId = builder.tableId;
    this.originalDatasetId = builder.originalDatasetId;
    this.originalProjectId = builder.originalProjectId;
    this.originalTableId = builder.originalTableId;
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

  /**
   * Gets the tableId to be updated to.
   *
   * <p>When {@code tableId} is null, do not update the tableId. Instead, use the tableId from the
   * {@code originalResource}.
   */
  public @Nullable String getTableId() {
    return tableId;
  }

  /** Gets the original referenced resources. */
  public String getOriginalTableId() {
    return originalTableId;
  }

  /** Gets the original BqDataset id that the reference is pointing to. */
  public String getOriginalDatasetId() {
    return originalDatasetId;
  }

  /** Gets the original BqDataset project id that the reference is pointing to. */
  public String getOriginalProjectId() {
    return originalProjectId;
  }

  /** Whether to update the target that the referenced resource is pointing to. */
  public boolean hasNewReferenceTargetFields() {
    return projectId != null || datasetId != null || tableId != null;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    public UpdateResourceParams resourceParams;
    private @Nullable String datasetId;
    private @Nullable String projectId;
    private @Nullable String tableId;
    private String originalDatasetId;
    private String originalProjectId;
    private String originalTableId;

    public UpdateReferencedBqTableParams.Builder resourceParams(
        UpdateResourceParams resourceParams) {
      this.resourceParams = resourceParams;
      return this;
    }

    public UpdateReferencedBqTableParams.Builder tableId(@Nullable String tableId) {
      this.tableId = tableId;
      return this;
    }

    public UpdateReferencedBqTableParams.Builder datasetId(@Nullable String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public UpdateReferencedBqTableParams.Builder projectId(@Nullable String projectId) {
      this.projectId = projectId;
      return this;
    }

    public UpdateReferencedBqTableParams.Builder originalTableId(String originalTableId) {
      this.originalTableId = originalTableId;
      return this;
    }

    public UpdateReferencedBqTableParams.Builder originalDatasetId(String originalDatasetId) {
      this.originalDatasetId = originalDatasetId;
      return this;
    }

    public UpdateReferencedBqTableParams.Builder originalProjectId(String originalProjectId) {
      this.originalProjectId = originalProjectId;
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
