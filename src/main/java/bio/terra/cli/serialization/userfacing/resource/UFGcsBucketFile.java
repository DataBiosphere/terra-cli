package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GcsBucketFile;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.GoogleCloudStorage;
import bio.terra.cli.utils.UserIO;
import bio.terra.cloudres.google.storage.BucketCow;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Optional;

/**
 * External representation of a workspace GCS bucket file resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GcsBucketFile} class for a bucket's internal representation.
 */
@JsonDeserialize(builder = UFGcsBucketFile.Builder.class)
public class UFGcsBucketFile extends UFResource {
  public final String bucketName;
  public final String bucketFileName;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcsBucketFile(GcsBucketFile internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.bucketFileName = internalObj.getBucketFileName();

    GoogleCloudStorage storage = GoogleCloudStorage.fromContextForPetSa();
    Optional<BucketCow> bucket = storage.getBucket(bucketName);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcsBucketFile(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.bucketFileName = builder.bucketFileName;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "GCS bucket name: " + bucketName + " file name: " + bucketFileName);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String bucketName;
    private String bucketFileName;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder bucketFileName(String bucketFileName) {
      this.bucketFileName = bucketFileName;
      return this;
    }

    /** Call the private constructor. */
    public UFGcsBucketFile build() {
      return new UFGcsBucketFile(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
