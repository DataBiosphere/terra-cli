package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating a Git repository workspace referenced resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = AddGitRepoParams.Builder.class)
public class AddGitRepoParams {
  public final CreateResourceParams resourceFields;
  public final String gitRepoUrl;

  protected AddGitRepoParams(AddGitRepoParams.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.gitRepoUrl = builder.gitRepoUrl;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateResourceParams resourceFields;
    private String gitRepoUrl;

    public Builder resourceFields(CreateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder gitRepoUrl(String gitRepoUrl) {
      this.gitRepoUrl = gitRepoUrl;
      return this;
    }

    /** Call the private constructor. */
    public AddGitRepoParams build() {
      return new AddGitRepoParams(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
