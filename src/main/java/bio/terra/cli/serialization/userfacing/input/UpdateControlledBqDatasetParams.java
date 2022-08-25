package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a BigQuery dataset workspace controlled resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateControlledBqDatasetParams {
  public final UpdateResourceParams resourceFields;
  public final Integer defaultPartitionLifetimeSeconds;
  public final Integer defaultTableLifetimeSeconds;
  public final CloningInstructionsEnum cloningInstructions;

  protected UpdateControlledBqDatasetParams(UpdateControlledBqDatasetParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.defaultPartitionLifetimeSeconds = builder.defaultPartitionLifetimeSeconds;
    this.defaultTableLifetimeSeconds = builder.defaultTableLifetimeSeconds;
    this.cloningInstructions = builder.cloningInstructions;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private Integer defaultPartitionLifetimeSeconds;
    private Integer defaultTableLifetimeSeconds;
    private CloningInstructionsEnum cloningInstructions;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UpdateControlledBqDatasetParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder defaultPartitionLifetimeSeconds(Integer defaultPartitionLifetimeSeconds) {
      this.defaultPartitionLifetimeSeconds = defaultPartitionLifetimeSeconds;
      return this;
    }

    public Builder defaultTableLifetimeSeconds(Integer defaultTableLifetimeSeconds) {
      this.defaultTableLifetimeSeconds = defaultTableLifetimeSeconds;
      return this;
    }

    public UpdateControlledBqDatasetParams.Builder cloningInstructions(
        CloningInstructionsEnum cloningInstructions) {
      this.cloningInstructions = cloningInstructions;
      return this;
    }

    /** Call the private constructor. */
    public UpdateControlledBqDatasetParams build() {
      return new UpdateControlledBqDatasetParams(this);
    }
  }
}
