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

  /** Serialize an instance of the internal class to the disk format. */
  public PDAwsBucket(AwsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
  }

  private PDAwsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public AwsBucket deserializeToInternal() {
    return new AwsBucket(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String bucketName;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    /** Call the private constructor. */
    public PDAwsBucket build() {
      return new PDAwsBucket(this);
    }
  }
}
