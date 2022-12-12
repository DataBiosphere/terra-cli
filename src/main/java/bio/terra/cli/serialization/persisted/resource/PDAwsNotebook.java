package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.AwsNotebook;
import bio.terra.cli.serialization.persisted.PDResource;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace AWS notebook resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link AwsNotebook} class for a notebook's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonDeserialize(builder = PDAwsNotebook.Builder.class)
public class PDAwsNotebook extends PDResource {
  public final String awsAccountNumber;
  public final String landingZoneId;
  public final String instanceId;
  public final String location;

  /** Serialize an instance of the internal class to the disk format. */
  public PDAwsNotebook(AwsNotebook internalObj) {
    super(internalObj);
    this.awsAccountNumber = internalObj.getAwsAccountNumber();
    this.landingZoneId = internalObj.getLandingZoneId();
    this.instanceId = internalObj.getInstanceId();
    this.location = internalObj.getLocation();
  }

  private PDAwsNotebook(Builder builder) {
    super(builder);
    this.awsAccountNumber = builder.awsAccountNumber;
    this.landingZoneId = builder.landingZoneId;
    this.instanceId = builder.instanceId;
    this.location = builder.location;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public AwsNotebook deserializeToInternal() {
    return new AwsNotebook(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private String awsAccountNumber;
    private String landingZoneId;
    private String instanceId;
    private String location;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder awsAccountNumber(String awsAccountNumber) {
      this.awsAccountNumber = awsAccountNumber;
      return this;
    }

    public Builder landingZoneId(String landingZoneId) {
      this.landingZoneId = landingZoneId;
      return this;
    }

    public Builder instanceId(String instanceId) {
      this.instanceId = instanceId;
      return this;
    }

    public Builder location(String location) {
      this.location = location;
      return this;
    }

    /** Call the private constructor. */
    public PDAwsNotebook build() {
      return new PDAwsNotebook(this);
    }
  }
}
