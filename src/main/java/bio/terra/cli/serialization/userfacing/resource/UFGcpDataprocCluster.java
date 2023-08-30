package bio.terra.cli.serialization.userfacing.resource;

import static bio.terra.cli.businessobject.resource.GcpDataprocCluster.JUPYTER_LAB_COMPONENT_KEY;

import bio.terra.axonserver.model.ClusterInstanceGroupConfig;
import bio.terra.axonserver.model.ClusterLifecycleConfig;
import bio.terra.axonserver.model.ClusterMetadata;
import bio.terra.axonserver.model.ClusterStatus;
import bio.terra.cli.businessobject.resource.GcpDataprocCluster;
import bio.terra.cli.serialization.userfacing.UFResource;
import bio.terra.cli.utils.UserIO;
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
  public final String projectId;
  public final String clusterId;
  public final String status;
  public final String proxyUri;
  public final int numWorkers;
  public final int numSecondaryWorkers;
  public final String autoscalingPolicy;
  public final Map<String, String> metadata;
  public final String idleDeleteTtl;

  /** Serialize an instance of the internal class to the command format. */
  public UFGcpDataprocCluster(GcpDataprocCluster internalObj) {
    super(internalObj);
    this.projectId = internalObj.getClusterName().projectId();
    this.clusterId = internalObj.getClusterName().name();

    // Fetch cluster status, proxy URL, and metadata
    Optional<ClusterStatus> status = internalObj.getClusterStatus();
    Optional<String> proxyUrl = internalObj.getClusterComponentUrl(JUPYTER_LAB_COMPONENT_KEY);
    Optional<ClusterMetadata> metadata = internalObj.getClusterMetadata();
    Optional<ClusterInstanceGroupConfig> workerConfig =
        metadata.map(ClusterMetadata::getPrimaryWorkerConfig);
    Optional<ClusterInstanceGroupConfig> secondaryWorkerConfig =
        metadata.map(ClusterMetadata::getSecondaryWorkerConfig);
    Optional<ClusterLifecycleConfig> lifecycleConfig =
        metadata.map(ClusterMetadata::getLifecycleConfig);

    // Set fields to display
    this.status = String.valueOf(status.map(ClusterStatus::getStatus).orElse(null));
    this.proxyUri = proxyUrl.orElse(null);
    this.numWorkers = workerConfig.map(ClusterInstanceGroupConfig::getNumInstances).orElse(0);
    this.numSecondaryWorkers =
        secondaryWorkerConfig.map(ClusterInstanceGroupConfig::getNumInstances).orElse(0);
    this.autoscalingPolicy = metadata.map(ClusterMetadata::getAutoscalingPolicy).orElse(null);
    this.metadata = metadata.map(ClusterMetadata::getMetadata).orElse(null);
    this.idleDeleteTtl = lifecycleConfig.map(ClusterLifecycleConfig::getIdleDeleteTtl).orElse(null);
  }

  /** Constructor for Jackson deserialization during testing. */
  private UFGcpDataprocCluster(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.clusterId = builder.clusterId;
    this.status = builder.status;
    this.proxyUri = builder.proxyUri;
    this.numWorkers = builder.numWorkers;
    this.numSecondaryWorkers = builder.numSecondaryWorkers;
    this.autoscalingPolicy = builder.autoscalingPolicy;
    this.metadata = builder.metadata;
    this.idleDeleteTtl = builder.idleDeleteTtl;
  }

  /** Print out this object in text format. */
  @Override
  public void print(String prefix) {
    super.print(prefix);
    PrintStream OUT = UserIO.getOut();
    OUT.println(prefix + "Project Id:   " + projectId);
    OUT.println(prefix + "Region:       " + region);
    OUT.println(prefix + "Cluster Id:   " + clusterId);
    OUT.println(prefix + "Status:       " + (status == null ? "(undefined)" : status));
    OUT.println(prefix + "Proxy URL:    " + (proxyUri == null ? "(undefined)" : proxyUri));
    OUT.println(prefix + "Workers:      " + numWorkers);
    OUT.println(prefix + "Secondary Workers:  " + numSecondaryWorkers);
    OUT.println(
        prefix
            + "Autoscaling Policy: "
            + (autoscalingPolicy == null ? "(undefined)" : autoscalingPolicy));
    if (metadata != null) {
      OUT.println(prefix + "Metadata:");
      metadata.forEach((key, value) -> OUT.println("   " + key + ": " + value));
    } else {
      OUT.println(prefix + "Metadata:           (undefined)");
    }
    OUT.println(
        prefix + "Idle Delete Ttl:   " + (idleDeleteTtl == null ? "(undefined)" : idleDeleteTtl));
  }

  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class Builder extends UFResource.Builder {
    private String projectId;
    private String clusterId;
    private String status;
    private String proxyUri;
    private int numWorkers;
    private int numSecondaryWorkers;
    private String autoscalingPolicy;
    private Map<String, String> metadata;
    private String idleDeleteTtl;

    /** Default constructor for Jackson. */
    public Builder() {}

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder clusterId(String clusterId) {
      this.clusterId = clusterId;
      return this;
    }

    public Builder status(String status) {
      this.status = status;
      return this;
    }

    public Builder proxyUri(String proxyUri) {
      this.proxyUri = proxyUri;
      return this;
    }

    public Builder numWorkers(int numWorkers) {
      this.numWorkers = numWorkers;
      return this;
    }

    public Builder numSecondaryWorkers(int numSecondaryWorkers) {
      this.numSecondaryWorkers = numSecondaryWorkers;
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

    public Builder idleDeleteTtl(String idleDeleteTtl) {
      this.idleDeleteTtl = idleDeleteTtl;
      return this;
    }

    /** Call the private constructor. */
    public UFGcpDataprocCluster build() {
      return new UFGcpDataprocCluster(this);
    }
  }
}
