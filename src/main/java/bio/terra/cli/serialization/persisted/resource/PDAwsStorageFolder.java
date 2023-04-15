package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.AwsStorageFolder;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace AWS storage folder resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link AwsStorageFolder} class for a storage folder's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDAwsStorageFolder.Builder.class)
public class PDAwsStorageFolder extends PDResource {
  public final String bucketName;
  public final String prefix;
  public final String region;

  /** Serialize an instance of the internal class to the disk format. */
  public PDAwsStorageFolder(AwsStorageFolder internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.prefix = internalObj.getPrefix();
    this.region = internalObj.getRegion();
  }

  private PDAwsStorageFolder(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.prefix = builder.prefix;
    this.region = builder.region;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public AwsStorageFolder deserializeToInternal() {
    return new AwsStorageFolder(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String bucketName;
    private String prefix;
    private String region;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder prefix(String prefix) {
      this.prefix = prefix;
      return this;
    }

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    /** Call the private constructor. */
    public PDAwsStorageFolder build() {
      return new PDAwsStorageFolder(this);
    }
  }
}
