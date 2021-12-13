package bio.terra.cli.serialization.userfacing.input.referenced;

import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import bio.terra.cli.serialization.userfacing.input.controlled.UpdateBqDatasetParams;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a BigQuery dataset workspace controlled resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedBqDatasetParams {
  private final UpdateResourceParams resourceParams;
  protected UpdateReferencedBqDatasetParams(UpdateReferencedBqDatasetParams.Builder builder){
    this.resourceParams = builder.resourceFields;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private UpdateResourceParams resourceFields;

    public UpdateReferencedBqDatasetParams.Builder resourceParams(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    /**
     * Call the private constructor.
     */
    public UpdateReferencedBqDatasetParams build() {
      return new UpdateReferencedBqDatasetParams(this);
    }

    /**
     * Default constructor for Jackson.
     */
    public Builder() {
    }
  }
}

