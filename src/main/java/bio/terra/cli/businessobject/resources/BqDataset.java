package bio.terra.cli.businessobject.resources;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resources.PDBqDataset;
import bio.terra.cli.serialization.userfacing.inputs.CreateBqDatasetParams;
import bio.terra.cli.serialization.userfacing.inputs.UpdateResourceParams;
import bio.terra.cli.serialization.userfacing.resources.UFBqDataset;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpBigQueryDatasetResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a Big Query dataset workspace resource. Instances of this class are
 * part of the current context or state.
 */
public class BqDataset extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(BqDataset.class);

  private String projectId;
  private String datasetId;

  /**
   * Delimiter between the project id and dataset id for a Big Query dataset.
   *
   * <p>The choice is somewhat arbitrary. BigQuery Datatsets do not have true URIs. The '.'
   * delimiter allows the path to be used directly in SQL calls with a Big Query extension.
   */
  private static final char BQ_PROJECT_DATASET_DELIMITER = '.';

  /** Deserialize an instance of the disk format to the internal object. */
  public BqDataset(PDBqDataset configFromDisk) {
    super(configFromDisk);
    this.projectId = configFromDisk.projectId;
    this.datasetId = configFromDisk.datasetId;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public BqDataset(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.BQ_DATASET;
    this.projectId = wsmObject.getResourceAttributes().getGcpBqDataset().getProjectId();
    this.datasetId = wsmObject.getResourceAttributes().getGcpBqDataset().getDatasetId();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public BqDataset(GcpBigQueryDatasetResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.BQ_DATASET;
    this.projectId = wsmObject.getAttributes().getProjectId();
    this.datasetId = wsmObject.getAttributes().getDatasetId();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFBqDataset serializeToCommand() {
    return new UFBqDataset(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDBqDataset serializeToDisk() {
    return new PDBqDataset(this);
  }

  /**
   * Add a Big Query dataset as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static BqDataset addReferenced(CreateBqDatasetParams createParams) {
    validateEnvironmentVariableName(createParams.resourceFields.name);

    // call WSM to add the reference
    GcpBigQueryDatasetResource addedResource =
        WorkspaceManagerService.fromContext()
            .createReferencedBigQueryDataset(Context.requireWorkspace().getId(), createParams);
    logger.info("Created BQ dataset: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new BqDataset(addedResource);
  }

  /**
   * Create a Big Query dataset as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static BqDataset createControlled(CreateBqDatasetParams createParams) {
    validateEnvironmentVariableName(createParams.resourceFields.name);

    // call WSM to create the resource
    GcpBigQueryDatasetResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledBigQueryDataset(Context.requireWorkspace().getId(), createParams);
    logger.info("Created BQ dataset: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new BqDataset(createdResource);
  }

  /** Update a Big Query dataset resource in the workspace. */
  public void update(UpdateResourceParams updateParams) {
    if (updateParams.name != null) {
      validateEnvironmentVariableName(updateParams.name);
    }
    switch (stewardshipType) {
      case REFERENCED:
        WorkspaceManagerService.fromContext()
            .updateReferencedBigQueryDataset(Context.requireWorkspace().getId(), id, updateParams);
        break;
      case CONTROLLED:
        WorkspaceManagerService.fromContext()
            .updateControlledBigQueryDataset(Context.requireWorkspace().getId(), id, updateParams);
        break;
      default:
        throw new IllegalArgumentException("Unknown stewardship type: " + stewardshipType);
    }
    super.updatePropertiesAndSync(updateParams);
  }

  /** Delete a Big Query dataset referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedBigQueryDataset(Context.requireWorkspace().getId(), id);
  }

  /** Delete a Big Query dataset controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerService.fromContext()
        .deleteControlledBigQueryDataset(Context.requireWorkspace().getId(), id);
  }

  /** This enum specifies the possible ways to resolve a Big Query dataset resource. */
  public enum ResolveOptions {
    FULL_PATH, // [project id].[dataset id]
    DATASET_ID_ONLY, // [dataset id]
    PROJECT_ID_ONLY; // [project id]
  }

  /**
   * Resolve a Big Query dataset resource to its cloud identifier. Returns the SQL path to the
   * dataset: [GCP project id].[BQ dataset id]
   */
  public String resolve() {
    return resolve(ResolveOptions.FULL_PATH);
  }

  /**
   * Resolve a Big Query dataset resource to its cloud identifier. Returns the SQL path to the
   * dataset: [GCP project id].[BQ dataset id]
   */
  public String resolve(ResolveOptions resolveOption) {
    switch (resolveOption) {
      case FULL_PATH:
        return projectId + BQ_PROJECT_DATASET_DELIMITER + datasetId;
      case DATASET_ID_ONLY:
        return datasetId;
      case PROJECT_ID_ONLY:
        return projectId;
      default:
        throw new IllegalArgumentException("Unknown Big Query dataset resolve option.");
    }
  }

  // ====================================================
  // Property getters.

  public String getProjectId() {
    return projectId;
  }

  public String getDatasetId() {
    return datasetId;
  }
}
