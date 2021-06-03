package bio.terra.cli.serialization.persisted.resources;

import bio.terra.cli.businessobject.resources.GcsBucket;
import bio.terra.cli.serialization.persisted.DiskResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace GCS bucket resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link GcsBucket} class for a bucket's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = DiskGcsBucket.Builder.class)
public class DiskGcsBucket extends DiskResource {
  public final String bucketName;

  /** Serialize an instance of the internal class to the disk format. */
  public DiskGcsBucket(GcsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
  }

  private DiskGcsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public GcsBucket deserializeToInternal() {
    return new GcsBucket(this);
  }

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
  }
}
