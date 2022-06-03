package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.CloningInstructionsEnum;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedGitRepoParams {
  public final UpdateResourceParams resourceFields;
  public final @Nullable String gitRepoUrl;
  public final @Nullable CloningInstructionsEnum cloningInstructions;

  protected UpdateReferencedGitRepoParams(UpdateReferencedGitRepoParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.gitRepoUrl = builder.gitRepoUrl;
    this.cloningInstructions = builder.cloningInstructions;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private @Nullable String gitRepoUrl;
    private @Nullable CloningInstructionsEnum cloningInstructions;

    public UpdateReferencedGitRepoParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateReferencedGitRepoParams.Builder gitRepoUrl(@Nullable String gitRepoUrl) {
      this.gitRepoUrl = gitRepoUrl;
      return this;
    }

    public UpdateReferencedGitRepoParams.Builder cloningInstructions(
        @Nullable CloningInstructionsEnum cloningInstructions) {
      this.cloningInstructions = cloningInstructions;
      return this;
    }

    /** Call the private constructor. */
    public UpdateReferencedGitRepoParams build() {
      return new UpdateReferencedGitRepoParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
