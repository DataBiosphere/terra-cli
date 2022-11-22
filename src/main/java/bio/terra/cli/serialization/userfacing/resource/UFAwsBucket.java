package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.AwsBucket;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a workspace AWS bucket resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AwsBucket} class for a bucket's internal representation.
 */
@JsonDeserialize(builder = UFAwsBucket.Builder.class)
public class UFAwsBucket extends UFResource {
  // the maximum number of objects to iterate through in the bucket.
  // if there are more, we just add a "+" at the end for display
  private static final long MAX_NUM_OBJECTS = 100;
  public final String bucketName;
  public final String location;
  public final Integer numObjects;

  /** Serialize an instance of the internal class to the command format. */
  public UFAwsBucket(AwsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.location = internalObj.getLocation();

    this.numObjects = 1;
/*
    // TODO(TERRA-207) add AWS account info - SA scope, proxy
    AmazonCloudStorage storage = AmazonCloudStorage.fromContextForPetSa();
    // TODO(TERRA-206) change to AWS BucketCow
    Optional<BucketCow> bucket = storage.getBucket(bucketName);
    this.numObjects =
        bucket
            .map((bucketCow) -> storage.getNumObjects(bucket.get(), MAX_NUM_OBJECTS + 1))
            .orElse(null);*/
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAwsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.location = builder.location;
    this.numObjects = builder.numObjects;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "AWS bucket name: " + bucketName);
    OUT.println(prefix + "Location: " + (location == null ? "(undefined)" : location));
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
    private String location;
    private Integer numObjects;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder numObjects(Integer numObjects) {
      this.numObjects = numObjects;
      return this;
    }

    /** Call the private constructor. */
    public UFAwsBucket build() {
      return new UFAwsBucket(this);
    }
  }
}
