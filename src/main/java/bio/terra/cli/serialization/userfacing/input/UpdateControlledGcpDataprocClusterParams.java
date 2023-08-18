package bio.terra.cli.serialization.userfacing.input;

import bio.terra.workspace.model.ControlledDataprocClusterUpdateParameters;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * Parameters for updating a GCP Dataproc cluster workspace controlled resource. This class is not
 * currently user-facing, but could be exposed as a command input format in the future.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@class")
@JsonDeserialize(builder = UpdateResourceParams.Builder.class)
public class UpdateControlledGcpDataprocClusterParams {
  /** When null, the name of the resource should not be updated. */
  public final UpdateResourceParams resourceFields;

  public final ControlledDataprocClusterUpdateParameters clusterUpdateParams;

  protected UpdateControlledGcpDataprocClusterParams(Builder builder) {
    this.resourceFields = builder.resourceFields;
    this.clusterUpdateParams = builder.clusterUpdateParams;
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder {
    private UpdateResourceParams resourceFields;
    private ControlledDataprocClusterUpdateParameters clusterUpdateParams;

    /** Default constructor for Jackson. */
    public Builder() {}

    public UpdateControlledGcpDataprocClusterParams.Builder resourceFields(
        UpdateResourceParams resourceFields) {
      this.resourceFields = resourceFields;
      return this;
    }

    public UpdateControlledGcpDataprocClusterParams.Builder clusterUpdateParams(
        ControlledDataprocClusterUpdateParameters clusterUpdateParams) {
      this.clusterUpdateParams = clusterUpdateParams;
      return this;
    }

    /** Call the private constructor. */
    public UpdateControlledGcpDataprocClusterParams build() {
      return new UpdateControlledGcpDataprocClusterParams(this);
    }
  }
}
