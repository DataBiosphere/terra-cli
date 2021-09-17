package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.WorkspaceUser;
import bio.terra.cli.utils.UserIO;
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
public class UFWorkspace implements UserFacing {
  public final UUID id;
  public final String name;
  public final String description;
  public final String googleProjectId;
  public final String serverName;
  public final String userEmail;
  public final long resourceCount;
  public final long userCount;

  /** Serialize an instance of the internal class to the disk format. */
  public UFWorkspace(Workspace internalObj) {
    this.id = internalObj.getId();
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.googleProjectId = internalObj.getGoogleProjectId();
    this.serverName = internalObj.getServerName();
    this.userEmail = internalObj.getUserEmail();
    this.resourceCount = internalObj.getResources().size();
    this.userCount = WorkspaceUser.list().size();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFWorkspace(Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.googleProjectId = builder.googleProjectId;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
    this.resourceCount = builder.resourceCount;
    this.userCount = builder.userCount;
  }

  /** Print out a workspace object in text format. */
  @Override
  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println("Terra workspace id: " + id);
    OUT.println("Display name:       " + name);
    OUT.println("Description:        " + description);
    OUT.println("Resource count:     " + resourceCount);
    OUT.println("User count:         " + userCount);
    OUT.println("Google project:     " + googleProjectId);
    OUT.println(
        "Cloud console:      https://console.cloud.google.com/home/dashboard?project="
            + googleProjectId);
  }

  public String getDeletePromptDescription() {
    return String.format(
        "This workspace contains %d resource(s) and %d user(s).", resourceCount, userCount);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UUID id;
    private String name;
    private String description;
    private String googleProjectId;
    private String serverName;
    private String userEmail;
    private long resourceCount;
    private long userCount;

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

    public Builder resourceCount(int resourceCount) {
      this.resourceCount = resourceCount;
      return this;
    }

    public Builder userCount(int userCount) {
      this.userCount = userCount;
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
