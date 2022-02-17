package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.Server;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.OffsetDateTime;

/**
 * External representation of a server for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link Server} class for a server's internal representation.
 */
@JsonDeserialize(builder = PDServer.Builder.class)
public class PDServer {
  public final String name;
  public final String description;
  public final String samUri;
  public final boolean samInviteRequiresAdmin;
  public final String workspaceManagerUri;
  public final String wsmDefaultSpendProfile;
  public final String dataRepoUri;
  public final OffsetDateTime lastVersionCheckTime;

  /** Serialize an instance of the internal class to the disk format. */
  public PDServer(Server internalObj) {
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.samUri = internalObj.getSamUri();
    this.samInviteRequiresAdmin = internalObj.getSamInviteRequiresAdmin();
    this.workspaceManagerUri = internalObj.getWorkspaceManagerUri();
    this.wsmDefaultSpendProfile = internalObj.getWsmDefaultSpendProfile();
    this.dataRepoUri = internalObj.getDataRepoUri();
    this.lastVersionCheckTime = internalObj.getLastVersionCheckTime();
  }

  private PDServer(PDServer.Builder builder) {
    this.name = builder.name;
    this.description = builder.description;
    this.samUri = builder.samUri;
    this.samInviteRequiresAdmin = builder.samInviteRequiresAdmin;
    this.workspaceManagerUri = builder.workspaceManagerUri;
    this.wsmDefaultSpendProfile = builder.wsmDefaultSpendProfile;
    this.dataRepoUri = builder.dataRepoUri;
    this.lastVersionCheckTime = builder.lastVersionCheckTime;
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
    private OffsetDateTime lastVersionCheckTime;

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

    public Builder lastVersionCheckTime(OffsetDateTime lastVersionCheckTime) {
      this.lastVersionCheckTime = lastVersionCheckTime;
      return this;
    }

    /** Call the private constructor. */
    public PDServer build() {
      return new PDServer(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
