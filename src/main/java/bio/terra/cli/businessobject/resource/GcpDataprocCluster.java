package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.persisted.resource.PDGcpDataprocCluster;
import bio.terra.cli.serialization.userfacing.input.CreateGcpDataprocClusterParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcpDataprocClusterParams;
import bio.terra.cli.serialization.userfacing.resource.UFGcpDataprocCluster;
import bio.terra.cli.service.WorkspaceManagerServiceGcp;
import bio.terra.cloudres.google.dataproc.ClusterName;
import bio.terra.workspace.model.GcpDataprocClusterResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a GCP Dataproc cluster workspace resource. Instances of this class are
 * part of the current context or state.
 */
public class GcpDataprocCluster extends Resource {
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

  public void updateControlled(UpdateControlledGcpDataprocClusterParams updateParams) {
    if (updateParams.resourceFields.name != null) {
      validateResourceName(updateParams.resourceFields.name);
    }
    WorkspaceManagerServiceGcp.fromContext()
        .updateControlledDataprocCluster(Context.requireWorkspace().getUuid(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceFields);
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

  /**
   * Resolve a GCP dataproc cluster resource to its cloud identifier. Return the cluster name
   * projects/[project_id]/regions/[region]/clusters/[clusterId].
   *
   * @return full name of the cluster
   */
  public String resolve() {
    return clusterName.formatName();
  }

  /** Query the cloud for information about the dataproc cluster VM. */
  // public Optional<Instance> getDataprocCluster() {
  // ClusterName clusterName =
  //     ClusterName.builder()
  //         .projectId(projectId)
  //         .region(region)
  //         .name(clusterId)
  //         .build();
  // GoogleNotebooks dataproc clusters = new
  // GoogleNotebooks(Context.requireUser().getPetSACredentials());
  // try {
  //   return Optional.of(dataproc clusters.get(clusterName));
  // } catch (Exception ex) {
  //   logger.error("Caught exception looking up dataproc cluster", ex);
  //   return Optional.empty();
  // }
  // }

  public ClusterName getClusterName() {
    return clusterName;
  }
}
