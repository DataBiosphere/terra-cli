package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.AwsS3StorageFolder;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a workspace AWS S3 Storage Folder resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AwsS3StorageFolder} class for a storage folder's internal representation.
 */
@JsonDeserialize(builder = UFAwsS3StorageFolder.Builder.class)
public class UFAwsS3StorageFolder extends UFResource {
  // the maximum number of objects to iterate through in the storage folder.
  // if there are more, we just add a "+" at the end for display
  private static final long MAX_NUM_OBJECTS = 100;
  public final String bucketName;
  public final String prefix;
  public final Integer numObjects;

  /** Serialize an instance of the internal class to the command format. */
  public UFAwsS3StorageFolder(AwsS3StorageFolder internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.prefix = internalObj.getPrefix();
    this.numObjects = internalObj.numObjects(MAX_NUM_OBJECTS + 1);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAwsS3StorageFolder(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.prefix = builder.prefix;
    this.numObjects = builder.numObjects;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(
        prefix + "S3 Storage Folder: " + AwsS3StorageFolder.resolve(bucketName, this.prefix, true));
    OUT.println(
        prefix
            + "# Objects: "
            + (numObjects == null
                ? "(unknown)"
                : (numObjects > MAX_NUM_OBJECTS ? MAX_NUM_OBJECTS + "+" : numObjects)));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String bucketName;
    private String prefix;
    private Integer numObjects;

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

    public Builder numObjects(Integer numObjects) {
      this.numObjects = numObjects;
      return this;
    }

    /** Call the private constructor. */
    public UFAwsS3StorageFolder build() {
      return new UFAwsS3StorageFolder(this);
    }
  }
}
