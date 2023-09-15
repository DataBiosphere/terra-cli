package bio.terra.cli.serialization.userfacing.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a GCP Dataproc cluster workspace controlled resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateControlledGcpDataprocClusterParams {
  /** When null, the name of the resource should not be updated. */
  public final UpdateResourceParams resourceFields;

  public final Integer numWorkers;
  public final Integer numSecondaryWorkers;
  public final String autoscalingPolicyUri;
  public final String gracefulDecommissionTimeout;
  public final String idleDeleteTtl;

  protected UpdateControlledGcpDataprocClusterParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.numWorkers = builder.numWorkers;
    this.numSecondaryWorkers = builder.numSecondaryWorkers;
    this.autoscalingPolicyUri = builder.autoscalingPolicyUri;
    this.gracefulDecommissionTimeout = builder.gracefulDecommissionTimeout;
    this.idleDeleteTtl = builder.idleDeleteTtl;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;

    private Integer numWorkers;
    private Integer numSecondaryWorkers;
    private String autoscalingPolicyUri;
    private String gracefulDecommissionTimeout;
    private String idleDeleteTtl;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UpdateControlledGcpDataprocClusterParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateControlledGcpDataprocClusterParams.Builder numWorkers(Integer numWorkers) {
      this.numWorkers = numWorkers;
      return this;
    }

    public UpdateControlledGcpDataprocClusterParams.Builder numSecondaryWorkers(
        Integer numSecondaryWorkers) {
      this.numSecondaryWorkers = numSecondaryWorkers;
      return this;
    }

    public UpdateControlledGcpDataprocClusterParams.Builder autoscalingPolicyUri(
        String autoscalingPolicyUri) {
      this.autoscalingPolicyUri = autoscalingPolicyUri;
      return this;
    }

    public UpdateControlledGcpDataprocClusterParams.Builder gracefulDecommissionTimeout(
        String gracefulDecommissionTimeout) {
      this.gracefulDecommissionTimeout = gracefulDecommissionTimeout;
      return this;
    }

    public UpdateControlledGcpDataprocClusterParams.Builder idleDeleteTtl(String idleDeleteTtl) {
      this.idleDeleteTtl = idleDeleteTtl;
      return this;
    }

    /** Call the private constructor. */
    public UpdateControlledGcpDataprocClusterParams build() {
      return new UpdateControlledGcpDataprocClusterParams(this);
    }
  }
}
