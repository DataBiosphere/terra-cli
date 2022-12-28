package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.AwsBucket;
import bio.terra.cli.cloud.aws.AwsStorageBucketsCow;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.utils.UserIO;
import bio.terra.workspace.model.AwsCredentialAccessScope;
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
  public final String bucketName;
  public final String bucketPrefix;
  public final String location;

  /** Serialize an instance of the internal class to the command format. */
  public UFAwsBucket(AwsBucket internalObj) {
    super(internalObj);
    this.bucketName = internalObj.getBucketName();
    this.bucketPrefix = internalObj.getBucketPrefix();
    this.location = internalObj.getLocation();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAwsBucket(Builder builder) {
    super(builder);
    this.bucketName = builder.bucketName;
    this.bucketPrefix = builder.bucketPrefix;
    this.location = builder.location;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "AWS bucket: " + AwsBucket.resolve(bucketName, bucketPrefix, true));
    OUT.println(prefix + "Location: " + (location == null ? "(undefined)" : location));
    // numObjects: not supported for AWS

    Workspace workspace = Context.requireWorkspace();
    AwsStorageBucketsCow buckets =
        AwsStorageBucketsCow.create(
            WorkspaceManagerService.fromContext()
                .getAwsBucketCredential(
                    workspace.getUuid(),
                    workspace.getResource(bucketPrefix).getId(),
                    AwsCredentialAccessScope.READ_ONLY,
                    WorkspaceManagerService.AWS_CREDENTIAL_EXPIRATION_SECONDS_DEFAULT),
            location);

    OUT.println(prefix + "DUMP: " + buckets.get(bucketName, bucketPrefix));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String bucketName;
    private String bucketPrefix;
    private String location;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder bucketName(String bucketName) {
      this.bucketName = bucketName;
      return this;
    }

    public Builder bucketPrefix(String bucketPrefix) {
      this.bucketPrefix = bucketPrefix;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public UFAwsBucket build() {
      return new UFAwsBucket(this);
    }
  }
}
