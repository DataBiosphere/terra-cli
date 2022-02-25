package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.UUID;

/**
 * External representation of a workspace for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Workspace} class for a workspace's internal representation.
 */
@JsonDeserialize(builder = UFWorkspace.Builder.class)
public class UFWorkspace {
  public final UUID id;
  public final String name;
  public final String description;
  public final String googleProjectId;
  public final String serverName;
  public final String userEmail;

  // only expose the number of resources in the text output mode
  @JsonIgnore public final long numResources;

  /** Serialize an instance of the internal class to the disk format. */
  public UFWorkspace(Workspace internalObj) {
    this.id = internalObj.getId();
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.googleProjectId = internalObj.getGoogleProjectId();
    this.serverName = internalObj.getServerName();
    this.userEmail = internalObj.getUserEmail();
    // This number may be zero for Workspace objects loaded from `WorkspaceDescription`s,
    // as those don't have any resources associated with them.
    this.numResources = internalObj.getResources().size();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFWorkspace(Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.googleProjectId = builder.googleProjectId;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
    this.numResources = builder.numResources;
  }

  /** Print out a workspace object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("Terra workspace id: " + id);
    OUT.println("Display name: " + name);
    OUT.println("Description: " + description);
    OUT.println("Google project: " + googleProjectId);
    OUT.println(
        "Cloud console: https://console.cloud.google.com/home/dashboard?project="
            + googleProjectId);
    // Unfortunately, this UFWorkspace may have been built from a Workspace object built from a
    // WorkspaceDescription, which does not provide the resources or their count. In that
    // situation, the number of resources will be zero, which may be false.
    // Currently, this only happens in `terra workspace list`, which prints separately,
    // so the user should not see it. `numResources` was removed from JSON output for
    // this reason (as we use the same JSON structure for list and describe).
    OUT.println("# Resources: " + numResources);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UUID id;
    private String name;
    private String description;
    private String googleProjectId;
    private String serverName;
    private String userEmail;
    private long numResources;

    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder googleProjectId(String googleProjectId) {
      this.googleProjectId = googleProjectId;
      return this;
    }

    public Builder serverName(String serverName) {
      this.serverName = serverName;
      return this;
    }

    public Builder userEmail(String userEmail) {
      this.userEmail = userEmail;
      return this;
    }

    public Builder numResources(long numResources) {
      this.numResources = numResources;
      return this;
    }

    /** Call the private constructor. */
    public UFWorkspace build() {
      return new UFWorkspace(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
