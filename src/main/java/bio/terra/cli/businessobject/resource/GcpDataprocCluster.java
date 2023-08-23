package bio.terra.cli.businessobject.resource;

import bio.terra.axonserver.model.ClusterMetadata;
import bio.terra.axonserver.model.ClusterStatus;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDGcpDataprocCluster;
import bio.terra.cli.serialization.userfacing.input.CreateGcpDataprocClusterParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import bio.terra.cli.service.AxonServerService;
import bio.terra.cli.service.WorkspaceManagerServiceGcp;
import bio.terra.cloudres.google.dataproc.ClusterName;
import bio.terra.workspace.model.GcpDataprocClusterResource;
import bio.terra.workspace.model.ResourceDescription;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a GCP Dataproc cluster workspace resource. Instances of this class are
 * part of the current context or state.
 */
public class GcpDataprocCluster extends Resource {
  public static final String JUPYTER_LAB_COMPONENT_KEY = "JupyterLab";
  private static final Logger logger = LoggerFactory.getLogger(GcpDataprocCluster.class);
  private final ClusterName clusterName;

  /** Deserialize a cluster of the disk format to the internal object. */
  public GcpDataprocCluster(PDGcpDataprocCluster configFromDisk) {
    super(configFromDisk);
    this.resourceType = Type.DATAPROC_CLUSTER;
    this.clusterName = configFromDisk.clusterName;
    this.region = configFromDisk.region;
  }

  /** Deserialize a cluster of the WSM client library object to the internal object. */
  public GcpDataprocCluster(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.DATAPROC_CLUSTER;
    this.clusterName =
        ClusterName.builder()
            .projectId(wsmObject.getResourceAttributes().getGcpDataprocCluster().getProjectId())
            .region(wsmObject.getResourceAttributes().getGcpDataprocCluster().getRegion())
            .name(wsmObject.getResourceAttributes().getGcpDataprocCluster().getClusterId())
            .build();
  }

  /** Deserialize a cluster of the WSM client library create object to the internal object. */
  public GcpDataprocCluster(GcpDataprocClusterResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.DATAPROC_CLUSTER;
    this.clusterName =
        ClusterName.builder()
            .projectId(wsmObject.getAttributes().getProjectId())
            .region(wsmObject.getAttributes().getRegion())
            .name(wsmObject.getAttributes().getClusterId())
            .build();
  }

  /**
   * Add a GCP dataproc cluster as a referenced resource in the workspace. Currently unsupported.
   */
  public static GcpDataprocCluster addReferenced(CreateGcpDataprocClusterParams createParams) {
    throw new UserActionableException(
        "Referenced resources not supported for GCP dataproc clusters.");
  }

  /**
   * Create a GCP dataproc cluster as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static GcpDataprocCluster createControlled(CreateGcpDataprocClusterParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    // call WSM to create the resource
    GcpDataprocClusterResource createdResource =
        WorkspaceManagerServiceGcp.fromContext()
            .createControlledGcpDataprocCluster(Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created GCP dataproc cluster: {}", createdResource);

    return new GcpDataprocCluster(createdResource);
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFGcpDataprocCluster serializeToCommand() {
    return new UFGcpDataprocCluster(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDGcpDataprocCluster serializeToDisk() {
    return new PDGcpDataprocCluster(this);
  }

  /** Delete a GCP dataproc cluster referenced resource in the workspace. Currently unsupported. */
  protected void deleteReferenced() {
    throw new UserActionableException(
        "Referenced resources not supported for GCP Dataproc clusters.");
  }

  /** Delete a GCP dataproc cluster controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerServiceGcp.fromContext()
        .deleteControlledGcpDataprocCluster(Context.requireWorkspace().getUuid(), id);
  }

  /** Retrieve cluster status from axon server */
  public Optional<ClusterStatus> getClusterStatus() {
    try {
      return Optional.of(
          AxonServerService.fromContext()
              .getClusterStatus(Context.requireWorkspace().getUuid(), id));
    } catch (Exception ex) {
      logger.error("Caught exception retrieving cluster status", ex);
      return Optional.empty();
    }
  }

  /**
   * Retrieve cluster component proxy url from axon server
   *
   * @param componentKey component key to retrieve proxy url for, e.g. "JupyterLab"
   */
  public Optional<bio.terra.axonserver.model.Url> getClusterComponentUrl(String componentKey) {
    try {
      return Optional.of(
          AxonServerService.fromContext()
              .getClusterComponentUrl(Context.requireWorkspace().getUuid(), id, componentKey));
    } catch (Exception ex) {
      logger.error("Caught exception retrieving cluster component", ex);
      return Optional.empty();
    }
  }

  /** Retrieve cluster attributes not stored in WSM from axon server */
  public Optional<ClusterMetadata> getClusterMetadata() {
    try {
      return Optional.of(
          AxonServerService.fromContext()
              .getClusterMetadata(Context.requireWorkspace().getUuid(), id));
    } catch (Exception ex) {
      logger.error("Caught exception querying dataproc cluster", ex);
      return Optional.empty();
    }
  }

  /**
   * Resolve a GCP dataproc cluster resource to its cloud identifier. Return the cluster name
   * projects/[project_id]/regions/[region]/clusters/[clusterId].
   *
   * @return full name of the cluster
   */
  public String resolve() {
    return clusterName.formatName();
  }

  public enum ProxyView {
    // These values are pulled from the HttpEndpointConfig from the Dataproc cluster api
    YARN_RESOURCE_MANAGER("YARN ResourceManager"),
    MAPREDUCE_JOB_HISTORY("MapReduce Job History"),
    SPARK_HISTORY_SERVER("Spark History Server"),
    HDFS_NAMENODE("HDFS NameNode"),
    YARN_APPLICATION_TIMELINE("YARN Application Timeline"),
    HIVESERVER2("HiveServer2 (all-components-m)"),
    TEZ("Tez"),
    JUPYTER("Jupyter"),
    JUPYTER_LAB("JupyterLab"),
    ZEPPELIN("Zeppelin"),
    SOLR("Solr"),
    FLINK_HISTORY_SERVER("Flink History Server");

    private final String param;

    ProxyView(String param) {
      this.param = param;
    }

    public String toParam() {
      return this.param;
    }
  }

  public ClusterName getClusterName() {
    return clusterName;
  }
}
