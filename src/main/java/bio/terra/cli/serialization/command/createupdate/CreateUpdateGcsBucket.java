package bio.terra.cli.serialization.command.createupdate;

import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for creating/updating a GCS bucket workspace resource. This class is not currently
 * user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateUpdateGcsBucket.Builder.class)
public class CreateUpdateGcsBucket {
  public final CreateUpdateResource resourceFields;
  public final String bucketName;
  public final GcsBucketLifecycle lifecycle;
  public final GcpGcsBucketDefaultStorageClass defaultStorageClass;
  public final String location;

  protected CreateUpdateGcsBucket(CreateUpdateGcsBucket.Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.bucketName = builder.bucketName;
    this.lifecycle = builder.lifecycle;
    this.defaultStorageClass = builder.defaultStorageClass;
    this.location = builder.location;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private CreateUpdateResource resourceFields;
    private String bucketName;
    private GcsBucketLifecycle lifecycle;
    private GcpGcsBucketDefaultStorageClass defaultStorageClass;
    private String location;

    public Builder resourceFields(CreateUpdateResource resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder lifecycle(GcsBucketLifecycle lifecycle) {
      this.lifecycle = lifecycle;
      return this;
    }

    public Builder defaultStorageClass(GcpGcsBucketDefaultStorageClass defaultStorageClass) {
      this.defaultStorageClass = defaultStorageClass;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public CreateUpdateGcsBucket build() {
      return new CreateUpdateGcsBucket(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
