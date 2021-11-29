package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GcsBucketFile;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

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
  public final String filePath;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcsBucketFile(GcsBucketFile internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.filePath = internalObj.getFilePath();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcsBucketFile(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.filePath = builder.filePath;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "GCS bucket name: " + bucketName + " file full path: " + filePath);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String bucketName;
    private String filePath;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder filePath(String filePath) {
      this.filePath = filePath;
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
