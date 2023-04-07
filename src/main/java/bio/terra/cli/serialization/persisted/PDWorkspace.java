package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.workspace.model.CloudPlatform;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.UUID;

/**
 * External representation of a workspace for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link Workspace} class for a workspace's internal representation.
 */
@JsonDeserialize(builder = PDWorkspace.Builder.class)
public class PDWorkspace {
  public final UUID uuid;
  public final String userFacingId;
  public final CloudPlatform cloudPlatform;

  // GCP
  public final String googleProjectId;

  // AWS
  public final String awsMajorVersion;
  public final String awsOrganizationId;
  public final String awsAccountId;
  public final String awsTenantAlias;
  public final String awsEnvironmentAlias;

  /** Serialize an instance of the internal class to the disk format. */
  public PDWorkspace(Workspace internalObj) {
    this.uuid = internalObj.getUuid();
    this.userFacingId = internalObj.getUserFacingId();
    this.cloudPlatform = internalObj.getCloudPlatform();
    this.googleProjectId = internalObj.getGoogleProjectId().orElse(null);
    this.awsMajorVersion = internalObj.getAwsMajorVersion().orElse(null);
    this.awsOrganizationId = internalObj.getAwsOrganizationId().orElse(null);
    this.awsAccountId = internalObj.getAwsAccountId().orElse(null);
    this.awsTenantAlias = internalObj.getAwsTenantAlias().orElse(null);
    this.awsEnvironmentAlias = internalObj.getAwsEnvironmentAlias().orElse(null);
  }

  private PDWorkspace(PDWorkspace.Builder builder) {
    this.uuid = builder.uuid;
    this.userFacingId = builder.userFacingId;
    this.cloudPlatform = builder.cloudPlatform;
    this.googleProjectId = builder.googleProjectId;
    this.awsMajorVersion = builder.awsMajorVersion;
    this.awsOrganizationId = builder.awsOrganizationId;
    this.awsAccountId = builder.awsAccountId;
    this.awsTenantAlias = builder.awsTenantAlias;
    this.awsEnvironmentAlias = builder.awsEnvironmentAlias;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UUID uuid;
    private String userFacingId;
    private CloudPlatform cloudPlatform;

    // GCP
    private String googleProjectId;

    // AWS
    private String awsMajorVersion;
    private String awsOrganizationId;
    private String awsAccountId;
    private String awsTenantAlias;
    private String awsEnvironmentAlias;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder uuid(UUID uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder userFacingId(String userFacingId) {
      this.userFacingId = userFacingId;
      return this;
    }

    public Builder cloudPlatform(CloudPlatform cloudPlatform) {
      this.cloudPlatform = cloudPlatform;
      return this;
    }

    public Builder googleProjectId(String googleProjectId) {
      this.googleProjectId = googleProjectId;
      return this;
    }

    public Builder awsMajorVersion(String awsMajorVersion) {
      this.awsMajorVersion = awsMajorVersion;
      return this;
    }

    public Builder awsOrganizationId(String awsOrganizationId) {
      this.awsOrganizationId = awsOrganizationId;
      return this;
    }

    public Builder awsAccountId(String awsAccountId) {
      this.awsAccountId = awsAccountId;
      return this;
    }

    public Builder awsTenantAlias(String awsTenantAlias) {
      this.awsTenantAlias = awsTenantAlias;
      return this;
    }

    public Builder awsEnvironmentAlias(String awsEnvironmentAlias) {
      this.awsEnvironmentAlias = awsEnvironmentAlias;
      return this;
    }

    /** Call the private constructor. */
    public PDWorkspace build() {
      return new PDWorkspace(this);
    }
  }
}
