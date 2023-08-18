package bio.terra.cli.serialization.persisted.resource;

import bio.terra.cli.businessobject.resource.GcpDataprocCluster;
import bio.terra.cli.serialization.persisted.PDResource;
import bio.terra.cloudres.google.dataproc.ClusterName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 * External representation of a workspace Dataproc Cluster resource for writing to disk.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is not user-facing.
 *
 * <p>See the {@link GcpDataprocCluster} class for a Dataproc cluster's internal representation.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@class")
@JsonDeserialize(builder = PDGcpDataprocCluster.Builder.class)
public class PDGcpDataprocCluster extends PDResource {
  public final ClusterName clusterName;

  /** Serialize an instance of the internal class to the disk format. */
  public PDGcpDataprocCluster(GcpDataprocCluster internalObj) {
    super(internalObj);
    this.clusterName = internalObj.getClusterName();
  }

  private PDGcpDataprocCluster(Builder builder) {
    super(builder);
    this.clusterName = builder.clusterName;
  }

  /** Deserialize the format for writing to disk to the internal representation of the resource. */
  public GcpDataprocCluster deserializeToInternal() {
    return new GcpDataprocCluster(this);
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends PDResource.Builder {
    private ClusterName clusterName;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder clusterName(ClusterName clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    /** Call the private constructor. */
    public PDGcpDataprocCluster build() {
      return new PDGcpDataprocCluster(this);
    }
  }
}
