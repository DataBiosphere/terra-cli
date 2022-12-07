package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.Server;
import bio.terra.workspace.model.CloudPlatform;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Set;

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
  public final String clientCredentialsFile;
  public final boolean cloudBuildEnabled;
  public final String description;
  public final String samUri;
  public final boolean samInviteRequiresAdmin;
  public final String workspaceManagerUri;
  public final String wsmDefaultSpendProfile;
  public final String dataRepoUri;
  public final String externalCredsUri;
  public final boolean supportsIdToken;
  public final Set<CloudPlatform> supportedCloudPlatforms;

  /** Serialize an instance of the internal class to the disk format. */
  public PDServer(Server internalObj) {
    this.name = internalObj.getName();
    this.clientCredentialsFile = internalObj.getClientCredentialsFile();
    this.cloudBuildEnabled = internalObj.getCloudBuildEnabled();
    this.description = internalObj.getDescription();
    this.samUri = internalObj.getSamUri();
    this.samInviteRequiresAdmin = internalObj.getSamInviteRequiresAdmin();
    this.workspaceManagerUri = internalObj.getWorkspaceManagerUri();
    this.wsmDefaultSpendProfile = internalObj.getWsmDefaultSpendProfile();
    this.dataRepoUri = internalObj.getDataRepoUri();
    this.externalCredsUri = internalObj.getExternalCredsUri();
    this.supportsIdToken = internalObj.getSupportsIdToken();
    this.supportedCloudPlatforms = internalObj.getSupportedCloudPlatforms();
  }

  private PDServer(PDServer.Builder builder) {
    this.name = builder.name;
    this.clientCredentialsFile = builder.clientCredentialsFile;
    this.cloudBuildEnabled = builder.cloudBuildEnabled;
    this.description = builder.description;
    this.samUri = builder.samUri;
    this.samInviteRequiresAdmin = builder.samInviteRequiresAdmin;
    this.workspaceManagerUri = builder.workspaceManagerUri;
    this.wsmDefaultSpendProfile = builder.wsmDefaultSpendProfile;
    this.dataRepoUri = builder.dataRepoUri;
    this.externalCredsUri = builder.externalCredsUri;
    this.supportsIdToken = builder.supportsIdToken;
    this.supportedCloudPlatforms = builder.supportedCloudPlatforms;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private String name;
    private String clientCredentialsFile;
    private boolean cloudBuildEnabled;
    private String description;
    private String samUri;
    private boolean samInviteRequiresAdmin;
    private String workspaceManagerUri;
    private String wsmDefaultSpendProfile;
    private String dataRepoUri;
    private String externalCredsUri;
    private boolean supportsIdToken;
    private Set<CloudPlatform> supportedCloudPlatforms;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder clientCredentialsFile(String clientCredentialsFile) {
      this.clientCredentialsFile = clientCredentialsFile;
      return this;
    }

    public Builder cloudBuildEnabled(boolean cloudBuildEnabled) {
      this.cloudBuildEnabled = cloudBuildEnabled;
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

    public Builder externalCredsUri(String externalCredsUri) {
      this.externalCredsUri = externalCredsUri;
      return this;
    }

    public Builder supportsIdToken(boolean supportsIdToken) {
      this.supportsIdToken = supportsIdToken;
      return this;
    }

    public Builder supportedCloudPlatforms(Set<CloudPlatform> supportedCloudPlatforms) {
      this.supportedCloudPlatforms = supportedCloudPlatforms;
      return this;
    }

    /** Call the private constructor. */
    public PDServer build() {
      return new PDServer(this);
    }
  }
}
