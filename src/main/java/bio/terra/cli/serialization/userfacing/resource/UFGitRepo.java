package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.businessobject.resource.GitRepository;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.GoogleCloudStorage;
import bio.terra.cli.utils.UserIO;
import bio.terra.cloudres.google.storage.BucketCow;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Optional;

/**
 * External representation of a workspace GCS bucket resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GitRepository} class for a bucket's internal representation.
 */
@JsonDeserialize(builder = UFGitRepository.Builder.class)
public class UFGitRepository extends UFResource {
  public final String gitRepoUrl;

  /** Serialize an instance of the internal class to the command format. */
  public UFGitRepository(GitRepository internalObj) {
    super(internalObj);
    this.gitRepoUrl = internalObj.getGitRepoUrl();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGitRepository(Builder builder) {
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

    public Builder gitRepoUrl(String gitRepoUrl) {
      this.gitRepoUrl = gitRepoUrl;
      return this;
    }

    /** Call the private constructor. */
    public UFGitRepository build() {
      return new UFGitRepository(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
