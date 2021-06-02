package bio.terra.cli.serialization.command.createupdate;

import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = CreateUpdateGcsBucket.Builder.class)
public class CreateUpdateGcsBucket extends CreateUpdateResource {
  public final String bucketName;
  public final GcsBucketLifecycle lifecycle;

  // storage class (https://cloud.google.com/storage/docs/storage-classes)
  public final GcpGcsBucketDefaultStorageClass defaultStorageClass;

  // bucket location (https://cloud.google.com/storage/docs/locations)
  public final String location;

  protected CreateUpdateGcsBucket(CreateUpdateGcsBucket.Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.lifecycle = builder.lifecycle;
    this.defaultStorageClass = builder.defaultStorageClass;
    this.location = builder.location;
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends CreateUpdateResource.Builder {
    private String bucketName;
    private GcsBucketLifecycle lifecycle;
    private GcpGcsBucketDefaultStorageClass defaultStorageClass;
    private String location;

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
