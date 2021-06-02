package bio.terra.cli.serialization.disk;

import bio.terra.cli.Workspace;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@JsonDeserialize(builder = DiskWorkspace.Builder.class)
public class DiskWorkspace {
  public final UUID id;
  public final String name;
  public final String description;
  public final String googleProjectId;
  public final String serverName;
  public final String userEmail;
  public final List<DiskResource> resources;

  private DiskWorkspace(DiskWorkspace.Builder builder) {
    this.id = builder.id;
    this.name = builder.name;
    this.description = builder.description;
    this.googleProjectId = builder.googleProjectId;
    this.serverName = builder.serverName;
    this.userEmail = builder.userEmail;
    this.resources = builder.resources;
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UUID id;
    private String name;
    private String description;
    private String googleProjectId;
    private String serverName;
    private String userEmail;
    private List<DiskResource> resources;

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

    public Builder resources(List<DiskResource> resources) {
      this.resources = resources;
      return this;
    }

    /** Call the private constructor. */
    public DiskWorkspace build() {
      return new DiskWorkspace(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the disk format. */
    public Builder(Workspace internalObj) {
      this.id = internalObj.getId();
      this.name = internalObj.getName();
      this.description = internalObj.getDescription();
      this.googleProjectId = internalObj.getGoogleProjectId();
      this.serverName = internalObj.getServerName();
      this.userEmail = internalObj.getUserEmail();
      this.resources =
          internalObj.getResources().stream()
              .map(resource -> resource.getResourceType().getDiskBuilder(resource).build())
              .collect(Collectors.toList());
    }
  }
}
