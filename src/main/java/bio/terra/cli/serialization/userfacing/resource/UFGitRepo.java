package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GitRepo;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a workspace git repo resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GitRepo} class for a git repo's internal representation.
 */
@JsonDeserialize(builder = UFGitRepo.Builder.class)
public class UFGitRepo extends UFResource {
  public final String gitRepoUrl;

  /** Serialize an instance of the internal class to the command format. */
  public UFGitRepo(GitRepo internalObj) {
    super(internalObj);
    this.gitRepoUrl = internalObj.getGitRepoUrl();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGitRepo(Builder builder) {
    super(builder);
    this.gitRepoUrl = builder.gitRepoUrl;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Git repo Url: " + gitRepoUrl);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String gitRepoUrl;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder gitRepoUrl(String gitRepoUrl) {
      this.gitRepoUrl = gitRepoUrl;
      return this;
    }

    /** Call the private constructor. */
    public UFGitRepo build() {
      return new UFGitRepo(this);
    }
  }
}
