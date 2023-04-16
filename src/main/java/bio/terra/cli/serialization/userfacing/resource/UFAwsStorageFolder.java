package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.AwsStorageFolder;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a workspace AWS storage folder resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AwsStorageFolder} class for a storage folder's internal representation.
 */
@JsonDeserialize(builder = UFAwsStorageFolder.Builder.class)
public class UFAwsStorageFolder extends UFResource {
  // the maximum number of objects to iterate through in the storage folder.
  // if there are more, we just add a "+" at the end for display
  private static final long MAX_NUM_OBJECTS = 100;
  public final String bucketName;
  public final String prefix;
  public final String region;
  public final Integer numObjects;

  /** Serialize an instance of the internal class to the command format. */
  public UFAwsStorageFolder(AwsStorageFolder internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.prefix = internalObj.getPrefix();
    this.region = internalObj.getRegion();
    this.numObjects = internalObj.numObjects(MAX_NUM_OBJECTS + 1);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAwsStorageFolder(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.prefix = builder.prefix;
    this.region = builder.region;
    this.numObjects = builder.numObjects;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(
        prefix + "AWS Storage Folder: " + AwsStorageFolder.resolve(bucketName, prefix, true));
    OUT.println(prefix + "Region: " + (region == null ? "(undefined)" : region));
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
    private String region;
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

    public Builder region(String region) {
      this.region = region;
      return this;
    }

    public Builder numObjects(Integer numObjects) {
      this.numObjects = numObjects;
      return this;
    }

    /** Call the private constructor. */
    public UFAwsStorageFolder build() {
      return new UFAwsStorageFolder(this);
    }
  }
}
