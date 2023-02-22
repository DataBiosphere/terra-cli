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
  public final String googleProjectId;
  public final String awsAccountNumber;
  public final String landingZoneId;
  public final CloudPlatform cloudPlatform;

  /** Serialize an instance of the internal class to the disk format. */
  public PDWorkspace(Workspace internalObj) {
    this.uuid = internalObj.getUuid();
    this.userFacingId = internalObj.getUserFacingId();
    this.googleProjectId = internalObj.getGoogleProjectId().orElse(null);
    this.awsAccountNumber = internalObj.getAwsAccountNumber().orElse(null);
    this.landingZoneId = internalObj.getLandingZoneId().orElse(null);
    this.cloudPlatform = internalObj.getCloudPlatform();
  }

  private PDWorkspace(PDWorkspace.Builder builder) {
    this.uuid = builder.uuid;
    this.userFacingId = builder.userFacingId;
    this.googleProjectId = builder.googleProjectId;
    this.awsAccountNumber = builder.awsAccountNumber;
    this.landingZoneId = builder.landingZoneId;
    this.cloudPlatform = builder.cloudPlatform;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UUID uuid;
    private String userFacingId;
    private String googleProjectId;
    private String awsAccountNumber;
    private String landingZoneId;
    private CloudPlatform cloudPlatform;

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

    public Builder googleProjectId(String googleProjectId) {
      this.googleProjectId = googleProjectId;
      return this;
    }

    public Builder awsAccountNumber(String awsAccountNumber) {
      this.awsAccountNumber = awsAccountNumber;
      return this;
    }

    public Builder landingZoneId(String landingZoneId) {
      this.landingZoneId = landingZoneId;
      return this;
    }

    public Builder cloudPlatform(CloudPlatform cloudPlatform) {
      this.cloudPlatform = cloudPlatform;
      return this;
    }

    /** Call the private constructor. */
    public PDWorkspace build() {
      return new PDWorkspace(this);
    }
  }
}
