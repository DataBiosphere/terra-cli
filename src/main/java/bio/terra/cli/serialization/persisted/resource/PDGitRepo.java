package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.businessobject.resource.GitRepository;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a reference to an external git repository.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link GcsObject} class for a bucket object's internal representation.
 */
public class PDGitRepository extends PDResource {
  public final String gitRepoUrl;

  public PDGitRepository(GitRepository internalObj) {
    super(internalObj);
    this.gitRepoUrl = internalObj.getGitRepoUrl();
  }

  private PDGitRepository(PDGitRepository.Builder builder) {
    super(builder);
    this.gitRepoUrl = builder.gitRepoUrl;
  }

  @Override
  public GitRepository deserializeToInternal() {
    return new GitRepository(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String gitRepoUrl;

    public PDGitRepository.Builder gitRepoUrl(String gitRepoUrl) {
      this.gitRepoUrl = gitRepoUrl;
      return this;
    }

    /** Call the private constructor. */
    public PDGitRepository build() {
      return new PDGitRepository(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
