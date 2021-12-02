package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GcsObject;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.GoogleCloudStorage;
import bio.terra.cli.utils.UserIO;
import bio.terra.cloudres.google.storage.BlobCow;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Optional;

/**
 * External representation of a workspace GCS bucket object resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GcsObject} class for a bucket's internal representation.
 */
@JsonDeserialize(builder = UFGcsObject.Builder.class)
public class UFGcsObject extends UFResource {

  public final String bucketName;
  public final String contentType;
  public final String objectName;
  public final Boolean isDirectory;
  public final Long size;
  public final Long timeStorageClassUpdated;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcsObject(GcsObject internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.objectName = internalObj.getObjectName();

    GoogleCloudStorage storage = GoogleCloudStorage.fromContextForPetSa();
    Optional<BlobCow> blob = storage.getBlob(bucketName, objectName);
    isDirectory = blob.map(blobCow -> blobCow.getBlobInfo().isDirectory()).orElse(null);
    size = blob.map(blobCow -> blobCow.getBlobInfo().getSize()).orElse(null);
    contentType = blob.map(blobCow -> blobCow.getBlobInfo().getContentType()).orElse(null);
    timeStorageClassUpdated =
        blob.map(blobCow -> blobCow.getBlobInfo().getTimeStorageClassUpdated()).orElse(null);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcsObject(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.objectName = builder.objectName;
    this.isDirectory = builder.isDirectory;
    this.size = builder.size;
    this.contentType = builder.contentType;
    this.timeStorageClassUpdated = builder.timeStorageClassUpdated;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "GCS bucket name: " + bucketName);
    OUT.println(prefix + "Full path to the object: " + objectName);
    OUT.println(prefix + "Content type: " + contentType == null ? "(Unknown)" : contentType);
    OUT.println(prefix + "Is directory: " + (isDirectory == null ? "(Unknown)" : isDirectory));
    OUT.println(prefix + "Size: " + (size == null ? "(Unknown)" : size));
    OUT.println(
        prefix
            + "The time that the object's storage class was last changed or the time of the object creation: "
            + (timeStorageClassUpdated == null ? "(Unknown)" : timeStorageClassUpdated));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {

    private String bucketName;
    private String contentType;
    private String objectName;
    private Boolean isDirectory;
    private Long size;
    private Long timeStorageClassUpdated;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public Builder objectName(String objectName) {
      this.objectName = objectName;
      return this;
    }

    public Builder isDirectory(Boolean isDirectory) {
      this.isDirectory = isDirectory;
      return this;
    }

    public Builder size(Long size) {
      this.size = size;
      return this;
    }

    public Builder timeStorageClassUpdated(Long timeStorageClassUpdated) {
      this.timeStorageClassUpdated = timeStorageClassUpdated;
      return this;
    }

    /** Call the private constructor. */
    public UFGcsObject build() {
      return new UFGcsObject(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
