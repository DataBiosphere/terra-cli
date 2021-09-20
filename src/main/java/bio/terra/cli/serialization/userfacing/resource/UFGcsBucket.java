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
  private static final long OBJECT_COUNT_LIMIT = 100;

  public final String bucketName;
  public final long objectCount;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcsBucket(GcsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.objectCount = internalObj.getObjectCount(OBJECT_COUNT_LIMIT);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.objectCount = builder.objectCount;
  }

  /** Print out this object in text format. */
  @Override
  public void print() {
    super.print();
    PrintStream OUT = UserIO.getOut();
    OUT.println("GCS bucket name: " + bucketName);
    OUT.println("Object count: " + objectCount + ((OBJECT_COUNT_LIMIT == objectCount) ? "+" : ""));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String bucketName;
    private long objectCount;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder objectCount(long objectCount) {
      this.objectCount = objectCount;
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
