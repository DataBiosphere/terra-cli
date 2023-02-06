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
  public final CloudPlatform cloudPlatform;

  /** Serialize an instance of the internal class to the disk format. */
  public PDWorkspace(Workspace internalObj) {
    this.uuid = internalObj.getUuid();
    this.userFacingId = internalObj.getUserFacingId();
    this.googleProjectId = internalObj.getGoogleProjectId().orElse(null);
    this.cloudPlatform = internalObj.getCloudPlatform();
  }

  private PDWorkspace(PDWorkspace.Builder builder) {
    this.uuid = builder.uuid;
    this.userFacingId = builder.userFacingId;
    this.googleProjectId = builder.googleProjectId;
    this.cloudPlatform = builder.cloudPlatform;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UUID uuid;
    private String userFacingId;
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

    public Builder cloudPlatform(CloudPlatform cloudPlatform) {
      this.cloudPlatform = cloudPlatform;
      return this;
    }

    public Builder spendProfile(String spendProfile) {
      this.spendProfile = spendProfile;
      return this;
    }

    /** Call the private constructor. */
    public PDWorkspace build() {
      return new PDWorkspace(this);
    }
  }
}
