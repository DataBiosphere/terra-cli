package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.GcsBucket;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.GoogleCloudStorage;
import bio.terra.cli.utils.UserIO;
import bio.terra.cloudres.google.storage.BucketCow;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Optional;

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
  public final String location;
  public final long numObjects;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcsBucket(GcsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();

    GoogleCloudStorage storage = GoogleCloudStorage.fromContextForPetSa();
    Optional<BucketCow> bucket = storage.getBucket(bucketName);
    this.location = bucket.isPresent() ? bucket.get().getBucketInfo().getLocation() : null;
    this.numObjects = bucket.isPresent() ? storage.getNumObjects(bucket.get()) : 0;
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcsBucket(Builder builder) {
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
    OUT.println(prefix + "GCS bucket name: " + bucketName);
    OUT.println(prefix + "Location: " + (location == null ? "(undefined)" : location));
    OUT.println(prefix + "# Objects: " + numObjects);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String bucketName;
    private String location;
    private long numObjects;

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    public Builder numObjects(long numObjects) {
      this.numObjects = numObjects;
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
