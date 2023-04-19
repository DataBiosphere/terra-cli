package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.AwsS3StorageFolder;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace AWS S3 Storage Folder resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link AwsS3StorageFolder} class for a storage folder's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDAwsS3StorageFolder.Builder.class)
public class PDAwsS3StorageFolder extends PDResource {
  public final String bucketName;
  public final String prefix;

  /** Serialize an instance of the internal class to the disk format. */
  public PDAwsS3StorageFolder(AwsS3StorageFolder internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.prefix = internalObj.getPrefix();
  }

  private PDAwsS3StorageFolder(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.prefix = builder.prefix;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public AwsS3StorageFolder deserializeToInternal() {
    return new AwsS3StorageFolder(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String bucketName;
    private String prefix;

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

    /** Call the private constructor. */
    public PDAwsS3StorageFolder build() {
      return new PDAwsS3StorageFolder(this);
    }
  }
}
