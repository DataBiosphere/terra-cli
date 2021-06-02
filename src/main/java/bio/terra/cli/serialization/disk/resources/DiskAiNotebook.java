package bio.terra.cli.serialization.disk.resources;

import bio.terra.cli.resources.AiNotebook;
import bio.terra.cli.serialization.disk.DiskResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = DiskAiNotebook.Builder.class)
public class DiskAiNotebook extends DiskResource {
  public final String projectId;
  public final String instanceId;
  public final String location;

  private DiskAiNotebook(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.instanceId = builder.instanceId;
    this.location = builder.location;
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends DiskResource.Builder {
    private String projectId;
    private String instanceId;
    private String location;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public DiskAiNotebook build() {
      return new DiskAiNotebook(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the disk format. */
    public Builder(AiNotebook internalObj) {
      super(internalObj);
      this.projectId = internalObj.getProjectId();
      this.instanceId = internalObj.getInstanceId();
      this.location = internalObj.getLocation();
    }
  }
}
