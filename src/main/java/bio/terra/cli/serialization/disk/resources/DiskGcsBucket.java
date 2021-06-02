package bio.terra.cli.serialization.disk.resources;

import bio.terra.cli.resources.GcsBucket;
import bio.terra.cli.serialization.disk.DiskResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = DiskGcsBucket.Builder.class)
public class DiskGcsBucket extends DiskResource {
  public final String bucketName;

  private DiskGcsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
  }

  /** Builder class to construct an immutable object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends DiskResource.Builder {
    private String bucketName;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    /** Call the private constructor. */
    public DiskGcsBucket build() {
      return new DiskGcsBucket(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}

    /** Serialize an instance of the internal class to the disk format. */
    public Builder(GcsBucket internalObj) {
      super(internalObj);
      this.bucketName = internalObj.getBucketName();
    }
  }
}
