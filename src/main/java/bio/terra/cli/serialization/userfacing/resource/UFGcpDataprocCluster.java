package bio.terra.cli.serialization.userfacing.resource;

import bio.terra.axonserver.model.ClusterInstanceGroupConfig;
import bio.terra.axonserver.model.ClusterMetadata;
import bio.terra.axonserver.model.ClusterStatus;
import bio.terra.cli.businessobject.resource.GcpDataprocCluster;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
import bio.terra.cloudres.google.dataproc.ClusterName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.io.PrintStream;
import java.util.Map;
import java.util.Optional;

/**
 * External representation of a workspace GCP Dataproc cluster resource for command input/output.
 *
 * <p>This is a POJO class intended for serialization. This JSON format is user-facing.
 *
 * <p>See the {@link GcpDataprocCluster} class for a Dataproc cluster's internal representation.
 */
@JsonDeserialize(builder = UFGcpDataprocCluster.Builder.class)
public class UFGcpDataprocCluster extends UFResource {
  private final String JUPYTER_LAB_COMPONENT_KEY = "JupyterLab";
  public final ClusterName clusterName;

  public final ClusterStatus status;
  public final String proxyUri;
  public final ClusterInstanceGroupConfig managerConfig;
  public final ClusterInstanceGroupConfig workerConfig;
  public final ClusterInstanceGroupConfig secondaryWorkerConfig;
  public final String autoscalingPolicy;
  public final Map<String, String> metadata;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcpDataprocCluster(GcpDataprocCluster internalObj) {
    super(internalObj);
    this.clusterName = internalObj.getClusterName();

    Optional<ClusterStatus> status = internalObj.getClusterStatus();
    Optional<bio.terra.axonserver.model.Url> proxyUrl =
        internalObj.getClusterComponentUrl(JUPYTER_LAB_COMPONENT_KEY);
    Optional<ClusterMetadata> metadata = internalObj.getClusterMetadata();

    this.status = status.orElse(null);
    this.proxyUri = proxyUrl.map(bio.terra.axonserver.model.Url::getUrl).orElse(null);
    this.autoscalingPolicy = metadata.map(ClusterMetadata::getAutoscalingPolicy).orElse(null);
    this.metadata = metadata.map(ClusterMetadata::getMetadata).orElse(null);
    this.managerConfig = metadata.map(ClusterMetadata::getManagerNodeConfig).orElse(null);
    this.workerConfig = metadata.map(ClusterMetadata::getPrimaryWorkerConfig).orElse(null);
    this.secondaryWorkerConfig =
        metadata.map(ClusterMetadata::getSecondaryWorkerConfig).orElse(null);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcpDataprocCluster(Builder builder) {
    super(builder);
    this.clusterName = builder.clusterName;
    this.status = builder.status;
    this.proxyUri = builder.proxyUri;
    this.autoscalingPolicy = builder.autoscalingPolicy;
    this.metadata = builder.metadata;
    this.managerConfig = builder.managerConfig;
    this.workerConfig = builder.workerConfig;
    this.secondaryWorkerConfig = builder.secondaryWorkerConfig;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Cluster Name: " + clusterName.name());
    // OUT.println(prefix + String.format("Manager Node: %s, %s", managerConfig.getNumInstances(),
    // managerConfig.getBootDiskSizeGb()));
    OUT.println(prefix + "Status: " + status.getStatus());
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private ClusterName clusterName;
    private ClusterStatus status;
    private String proxyUri;
    private String autoscalingPolicy;
    private Map<String, String> metadata;
    private ClusterInstanceGroupConfig managerConfig;
    private ClusterInstanceGroupConfig workerConfig;
    private ClusterInstanceGroupConfig secondaryWorkerConfig;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder clusterName(ClusterName clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder status(ClusterStatus status) {
      this.status = status;
      return this;
    }

    public Builder proxyUri(String proxyUri) {
      this.proxyUri = proxyUri;
      return this;
    }

    public Builder autoscalingPolicy(String autoscalingPolicy) {
      this.autoscalingPolicy = autoscalingPolicy;
      return this;
    }

    public Builder metadata(Map<String, String> metadata) {
      this.metadata = metadata;
      return this;
    }

    public Builder managerConfig(ClusterInstanceGroupConfig managerConfig) {
      this.managerConfig = managerConfig;
      return this;
    }

    public Builder workerConfig(ClusterInstanceGroupConfig workerConfig) {
      this.workerConfig = workerConfig;
      return this;
    }

    public Builder secondaryWorkerConfig(ClusterInstanceGroupConfig secondaryWorkerConfig) {
      this.secondaryWorkerConfig = secondaryWorkerConfig;
      return this;
    }

    /** Call the private constructor. */
    public UFGcpDataprocCluster build() {
      return new UFGcpDataprocCluster(this);
    }
  }
}
