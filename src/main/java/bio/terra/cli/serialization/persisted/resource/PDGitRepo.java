package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.GitRepo;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a reference to an external git repository.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link GitRepo} class for a git repo's internal representation.
 */
public class PDGitRepo extends PDResource {
  public final String gitRepoUrl;

  public PDGitRepo(GitRepo internalObj) {
    super(internalObj);
    this.gitRepoUrl = internalObj.getGitRepoUrl();
  }

  private PDGitRepo(PDGitRepo.Builder builder) {
    super(builder);
    this.gitRepoUrl = builder.gitRepoUrl;
  }

  @Override
  public GitRepo deserializeToInternal() {
    return new GitRepo(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String gitRepoUrl;

    public PDGitRepo.Builder gitRepoUrl(String gitRepoUrl) {
      this.gitRepoUrl = gitRepoUrl;
      return this;
    }

    /** Call the private constructor. */
    public PDGitRepo build() {
      return new PDGitRepo(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
