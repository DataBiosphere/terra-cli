package bio.terra.cli.serialization.userfacing.input.referenced;

import bio.terra.cli.businessobject.resource.BqTable;
import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
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
  private final BqTable originalResource;

  protected UpdateReferencedBqTableParams(UpdateReferencedBqTableParams.Builder builder) {
    this.resourceParams = builder.resourceParams;
    this.datasetId = builder.datasetId;
    this.projectId = builder.projectId;
    this.tableId = builder.tableId;
    this.originalResource = builder.originalResource;
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
  public BqTable getOriginalResource() {
    return originalResource;
  }

  /**
   * Whether to update the target that the referenced resource is pointing to.
   *
   * @return
   */
  public boolean updateReferenceTarget() {
    return projectId != null || datasetId != null || tableId != null;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    public UpdateResourceParams resourceParams;
    private @Nullable String datasetId;
    private @Nullable String projectId;
    private @Nullable String tableId;
    private BqTable originalResource;

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

    public UpdateReferencedBqTableParams.Builder originalResource(BqTable originalResource) {
      this.originalResource = originalResource;
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
