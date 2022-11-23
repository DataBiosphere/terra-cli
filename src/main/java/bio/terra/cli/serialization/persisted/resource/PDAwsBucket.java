package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.AwsBucket;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace AWS bucket resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link AwsBucket} class for a bucket's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDAwsBucket.Builder.class)
public class PDAwsBucket extends PDResource {
  public final String bucketName;
  public final String bucketPrefix;
  public final String location;

  /** Serialize an instance of the internal class to the disk format. */
  public PDAwsBucket(AwsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.bucketPrefix = internalObj.getBucketPrefix();
    this.location = internalObj.getLocation();
  }

  private PDAwsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.bucketPrefix = builder.bucketPrefix;
    this.location = builder.location;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public AwsBucket deserializeToInternal() {
    return new AwsBucket(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String bucketName;
    private String bucketPrefix;
    private String location;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder bucketPrefix(String bucketPrefix) {
      this.bucketPrefix = bucketPrefix;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public PDAwsBucket build() {
      return new PDAwsBucket(this);
    }
  }
}
