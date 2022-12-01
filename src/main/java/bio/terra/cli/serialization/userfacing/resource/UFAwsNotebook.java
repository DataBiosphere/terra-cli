package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.cli.businessobject.resource.AwsNotebook;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;

/**
 * External representation of a workspace AWS notebook resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link AwsNotebook} class for a notebook's internal representation.
 */
@JsonDeserialize(builder = UFAwsNotebook.Builder.class)
public class UFAwsNotebook extends UFResource {
  public final String instanceId;
  public final String location;

  /** Serialize an instance of the internal class to the command format. */
  public UFAwsNotebook(AwsNotebook internalObj) {
    super(internalObj);
    this.instanceId = internalObj.getInstanceId();
    this.location = internalObj.getLocation();
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFAwsNotebook(Builder builder) {
    super(builder);
    this.instanceId = builder.instanceId;
    this.location = builder.location;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Instance id:   " + instanceId);
    OUT.println(prefix + "Location: " + (location == null ? "(undefined)" : location));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String instanceId;
    private String location;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public UFAwsNotebook build() {
      return new UFAwsNotebook(this);
    }
  }
}
