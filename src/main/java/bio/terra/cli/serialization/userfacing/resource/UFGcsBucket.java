package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a workspace GCS bucket resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GcsBucket} class for a bucket's internal representation.
 */
@JsonDeserialize(builder = UFGcsBucket.Builder.class)
public class UFGcsBucket extends UFResource {
  public final String bucketName;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcsBucket(GcsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
  }

  /** Print out this object in text format. */
  @Override
  public void print() {
    super.print();
    PrintStream OUT = UserIO.getOut();
    OUT.println("GCS bucket name: " + bucketName);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String bucketName;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    /** Call the private constructor. */
    public UFGcsBucket build() {
      return new UFGcsBucket(this);
    }

    /** Default constructor for Jackson. */
    public Builder() {}
  }
}
