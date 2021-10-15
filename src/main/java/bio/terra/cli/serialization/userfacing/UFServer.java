package bio.terra.cli.serialization.userfacing;

import bio.terra.cli.businessobject.Server;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a server for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link Server} class for a server's internal representation.
 */
@JsonDeserialize(builder = UFServer.Builder.class)
public class UFServer {
  public final String name;
  public final String description;
  public final String samUri;
  public final boolean samInviteRequiresAdmin;
  public final String workspaceManagerUri;
  public final String wsmDefaultSpendProfile;
  public final String dataRepoUri;

  /** Serialize an instance of the internal class to the command format. */
  public UFServer(Server internalObj) {
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.samUri = internalObj.getSamUri();
    this.samInviteRequiresAdmin = internalObj.getSamInviteRequiresAdmin();
    this.workspaceManagerUri = internalObj.getWorkspaceManagerUri();
    this.wsmDefaultSpendProfile = internalObj.getWsmDefaultSpendProfile();
    this.dataRepoUri = internalObj.getDataRepoUri();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFServer(Builder builder) {
    this.name = builder.name;
    this.description = builder.description;
    this.samUri = builder.samUri;
    this.samInviteRequiresAdmin = builder.samInviteRequiresAdmin;
    this.workspaceManagerUri = builder.workspaceManagerUri;
    this.wsmDefaultSpendProfile = builder.wsmDefaultSpendProfile;
    this.dataRepoUri = builder.dataRepoUri;
  }

  /** Print out this object in text format. */
  public void print() {
    PrintStream OUT = UserIO.getOut();
    OUT.println(name + ": " + description);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String description;
    private String samUri;
    private boolean samInviteRequiresAdmin;
    private String workspaceManagerUri;
    private String wsmDefaultSpendProfile;
    private String dataRepoUri;

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder samUri(String samUri) {
      this.samUri = samUri;
      return this;
    }

    public Builder samInviteRequiresAdmin(boolean samInviteRequiresAdmin) {
      this.samInviteRequiresAdmin = samInviteRequiresAdmin;
      return this;
    }

    public Builder workspaceManagerUri(String workspaceManagerUri) {
      this.workspaceManagerUri = workspaceManagerUri;
      return this;
    }

    public Builder wsmDefaultSpendProfile(String wsmDefaultSpendProfile) {
      this.wsmDefaultSpendProfile = wsmDefaultSpendProfile;
      return this;
    }

    public Builder dataRepoUri(String dataRepoUri) {
      this.dataRepoUri = dataRepoUri;
      return this;
    }

    /** Call the private constructor. */
    public UFServer build() {
      return new UFServer(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
