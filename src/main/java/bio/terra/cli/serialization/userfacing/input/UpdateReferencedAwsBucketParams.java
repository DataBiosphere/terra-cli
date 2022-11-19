package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a AWS bucket workspace referenced resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedAwsBucketParams {
  public final UpdateResourceParams resourceParams;
  public final String bucketName;
  public final CloningInstructionsEnum cloningInstructions;

  protected UpdateReferencedAwsBucketParams(UpdateReferencedAwsBucketParams.Builder builder) {
    this.resourceParams = builder.resourceFields;
    this.bucketName = builder.bucketName;
    this.cloningInstructions = builder.cloningInstructions;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {

    private UpdateResourceParams resourceFields;
    private String bucketName;
    private CloningInstructionsEnum cloningInstructions;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UpdateReferencedAwsBucketParams.Builder resourceParams(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateReferencedAwsBucketParams.Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public UpdateReferencedAwsBucketParams.Builder cloningInstructions(
        CloningInstructionsEnum cloningInstructions) {
      this.cloningInstructions = cloningInstructions;
      return this;
    }

    /** Call the private constructor. */
    public UpdateReferencedAwsBucketParams build() {
      return new UpdateReferencedAwsBucketParams(this);
    }
  }
}
