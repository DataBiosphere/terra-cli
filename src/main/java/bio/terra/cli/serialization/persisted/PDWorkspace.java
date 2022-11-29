package bio.terra.cli.serialization.persisted;

import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
  public final String name;
  public final String description;
  public final String googleProjectId;
  public final String awsAccountNumber;
  public final String landingZoneId;
  public final Map<String, String> properties;
  public final String serverName;
  public final String userEmail;
  public final List<PDResource> resources;
  public final OffsetDateTime createdDate;
  public final OffsetDateTime lastUpdatedDate;

  /** Serialize an instance of the internal class to the disk format. */
  public PDWorkspace(Workspace internalObj) {
    this.uuid = internalObj.getUuid();
    this.userFacingId = internalObj.getUserFacingId();
    this.name = internalObj.getName();
    this.description = internalObj.getDescription();
    this.googleProjectId = internalObj.getGoogleProjectId();
    this.awsAccountNumber = internalObj.getAwsAccountNumber();
    this.landingZoneId = internalObj.getLandingZoneId();
    this.properties = internalObj.getProperties();
    this.serverName = internalObj.getServerName();
    this.userEmail = internalObj.getUserEmail();
    this.resources =
        internalObj.getResources().stream()
            .map(Resource::serializeToDisk)
            .collect(Collectors.toList());
    this.createdDate = internalObj.getCreatedDate();
    this.lastUpdatedDate = internalObj.getLastUpdatedDate();
  }

  private PDWorkspace(PDWorkspace.Builder builder) {
    this.uuid = builder.uuid;
    this.userFacingId = builder.userFacingId;
    this.name = builder.name;
    this.description = builder.description;
    this.googleProjectId = builder.googleProjectId;
    this.awsAccountNumber = builder.awsAccountNumber;
    this.landingZoneId = builder.landingZoneId;
    this.properties = builder.properties;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
    this.resources = builder.resources;
    this.createdDate = builder.createdDate;
    this.lastUpdatedDate = builder.lastUpdatedDate;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UUID uuid;
    private String userFacingId;
    private String name;
    private String description;
    private String googleProjectId;
    private String awsAccountNumber;
    private String landingZoneId;
    private Map<String, String> properties;
    private String serverName;
    private String userEmail;
    private List<PDResource> resources;
    private OffsetDateTime createdDate;
    private OffsetDateTime lastUpdatedDate;

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

    public Builder awsAccountNumber(String awsAccountNumber) {
      this.awsAccountNumber = awsAccountNumber;
      return this;
    }

    public Builder landingZoneId(String landingZoneId) {
      this.landingZoneId = landingZoneId;
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

    public Builder properties(Map<String, String> properties) {
      this.properties = properties;
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

    public Builder resources(List<PDResource> resources) {
      this.resources = resources;
      return this;
    }

    public Builder createdDate(OffsetDateTime createdDate) {
      this.createdDate = createdDate;
      return this;
    }

    public Builder lastUpdatedDate(OffsetDateTime lastUpdatedDate) {
      this.lastUpdatedDate = lastUpdatedDate;
      return this;
    }

    /** Call the private constructor. */
    public PDWorkspace build() {
      return new PDWorkspace(this);
    }
  }
}
