package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import javax.annotation.Nullable;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateReferencedGitRepoParams {
  public final UpdateResourceParams resourceFields;
  public final @Nullable String gitRepoUrl;

  protected UpdateReferencedGitRepoParams(UpdateReferencedGitRepoParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.gitRepoUrl = builder.gitRepoUrl;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private @Nullable String gitRepoUrl;

    public UpdateReferencedGitRepoParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateReferencedGitRepoParams.Builder gitRepoUrl(@Nullable String gitRepoUrl) {
      this.gitRepoUrl = gitRepoUrl;
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
