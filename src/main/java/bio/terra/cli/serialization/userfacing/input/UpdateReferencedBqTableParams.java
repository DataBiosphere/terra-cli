package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.CloningInstructionsEnum;
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
  /**
   * New tableId to be updated to.
   *
   * <p>When {@code tableId} is null, do not update the tableId. Instead, use the tableId from the
   * {@code originalResource}.
   */
  public final @Nullable String tableId;

  public final @Nullable CloningInstructionsEnum cloningInstructions;

  protected UpdateReferencedBqTableParams(UpdateReferencedBqTableParams.Builder builder) {
    this.resourceParams = builder.resourceParams;
    this.datasetId = builder.datasetId;
    this.projectId = builder.projectId;
    this.tableId = builder.tableId;
    this.cloningInstructions = builder.cloningInstructions;
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
    private @Nullable CloningInstructionsEnum cloningInstructions;

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

    public UpdateReferencedBqTableParams.Builder cloningInstructions(
        @Nullable CloningInstructionsEnum cloningInstructions) {
      this.cloningInstructions = cloningInstructions;
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
