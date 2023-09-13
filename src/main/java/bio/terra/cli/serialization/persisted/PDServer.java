package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.Server;
import bio.terra.workspace.model.CloudPlatform;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.Collections;
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
  public final String userManagerUri;
  public final String axonServerUri;
  public final boolean supportsIdToken;
  public final Set<CloudPlatform> supportedCloudPlatforms;
  public final boolean auth0Enabled;
  public final String auth0Domain;

  public final String flagsmithApiUrl;

  public final String flagsmithClientSideKey;

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
    this.userManagerUri = internalObj.getUserManagerUri();
    this.axonServerUri = internalObj.getAxonServerUri();
    this.supportsIdToken = internalObj.getSupportsIdToken();
    this.supportedCloudPlatforms = internalObj.getSupportedCloudPlatforms();
    this.auth0Enabled = internalObj.getAuth0Enabled();
    this.auth0Domain = internalObj.getAuth0Domain();
    this.flagsmithApiUrl = internalObj.getFlagsmithApiUrl();
    this.flagsmithClientSideKey = internalObj.getFlagsmithClientSideKey();
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
    this.userManagerUri = builder.userManagerUri;
    this.axonServerUri = builder.axonServerUri;
    this.supportsIdToken = builder.supportsIdToken;
    this.supportedCloudPlatforms =
        builder.supportedCloudPlatforms != null
            ? builder.supportedCloudPlatforms
            : Collections.EMPTY_SET;
    this.auth0Enabled = builder.auth0Enabled;
    this.auth0Domain = builder.auth0Domain;
    this.flagsmithApiUrl = builder.flagsmithApiUrl;
    this.flagsmithClientSideKey = builder.flagsmithClientSideKey;
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
    private String userManagerUri;
    private String axonServerUri;
    private boolean supportsIdToken;
    private Set<CloudPlatform> supportedCloudPlatforms;

    private boolean auth0Enabled;

    private String auth0Domain;

    private String flagsmithApiUrl;

    private String flagsmithClientSideKey;

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

    public Builder userManagerUri(String userManagerUri) {
      this.userManagerUri = userManagerUri;
      return this;
    }

    public Builder axonServerUri(String axonServerUri) {
      this.axonServerUri = axonServerUri;
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

    public Builder auth0Enabled(boolean auth0Enabled) {
      this.auth0Enabled = auth0Enabled;
      return this;
    }

    public Builder auth0Domain(String auth0Domain) {
      this.auth0Domain = auth0Domain;
      return this;
    }

    public Builder flagsmithApiUrl(String flagsmithApiUrl) {
      this.flagsmithApiUrl = flagsmithApiUrl;
      return this;
    }

    public Builder flagsmithClientSideKey(String flagsmithClientSideKey) {
      this.flagsmithClientSideKey = flagsmithClientSideKey;
      return this;
    }

    /** Call the private constructor. */
    public PDServer build() {
      return new PDServer(this);
    }
  }
}
