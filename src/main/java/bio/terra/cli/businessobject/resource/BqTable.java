package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDBqTable;
import bio.terra.cli.serialization.userfacing.input.AddBqTableParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedBqTableParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqTable;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpBigQueryDataTableResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a BigQuery data table workspace resource. Instances of this class are
 * part of the current context or state.
 */
public class BqTable extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(BqTable.class);
  private String projectId;
  private String datasetId;
  private String dataTableId;

  /** Deserialize an instance of the disk format to the internal object. */
  public BqTable(PDBqTable configFromDisk) {
    super(configFromDisk);
    this.projectId = configFromDisk.projectId;
    this.datasetId = configFromDisk.datasetId;
    this.dataTableId = configFromDisk.dataTableId;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public BqTable(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.BQ_TABLE;
    this.projectId = wsmObject.getResourceAttributes().getGcpBqDataTable().getProjectId();
    this.datasetId = wsmObject.getResourceAttributes().getGcpBqDataTable().getDatasetId();
    this.dataTableId = wsmObject.getResourceAttributes().getGcpBqDataTable().getDataTableId();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public BqTable(GcpBigQueryDataTableResource resource) {
    super(resource.getMetadata());
    this.resourceType = Type.BQ_TABLE;
    this.projectId = resource.getAttributes().getProjectId();
    this.datasetId = resource.getAttributes().getDatasetId();
    this.dataTableId = resource.getAttributes().getDataTableId();
  }

  /**
   * Add a BigQuery data table as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static BqTable addReferenced(AddBqTableParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    GcpBigQueryDataTableResource addedResource =
        WorkspaceManagerService.fromContext()
            .createReferencedBigQueryDataTable(Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created BQ data table: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new BqTable(addedResource);
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFBqTable serializeToCommand() {
    return new UFBqTable(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDBqTable serializeToDisk() {
    return new PDBqTable(this);
  }

  /** Update a BigQuery data table referenced resource in the workspace. */
  public void updateReferenced(UpdateReferencedBqTableParams updateParams) {
    if (updateParams.resourceParams.name != null) {
      validateResourceName(updateParams.resourceParams.name);
    }
    if (updateParams.projectId != null) {
      this.projectId = updateParams.projectId;
    }
    if (updateParams.datasetId != null) {
      this.datasetId = updateParams.datasetId;
    }
    if (updateParams.tableId != null) {
      this.dataTableId = updateParams.tableId;
    }
    if (updateParams.cloningInstructions != null) {
      this.cloningInstructions = updateParams.cloningInstructions;
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedBigQueryDataTable(Context.requireWorkspace().getUuid(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceParams);
  }

  /** Delete a BigQuery data table referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedBigQueryDataTable(Context.requireWorkspace().getUuid(), id);
  }

  /** Delete a BigQuery data table controlled resource in the workspace. */
  protected void deleteControlled() {
    throw new UnsupportedOperationException(
        "Unsupport this operation because workspace manager does not support creating a controlled BigQuery data table");
  }

  /**
   * Resolve a BigQuery data table resource to its cloud identifier. Returns the SQL path to the
   * data table: [GCP project id].[BQ dataset id].[BQ data table id]
   */
  public String resolve() {
    return resolve(BqResolvedOptions.FULL_PATH);
  }

  /**
   * Resolve a BigQuery data table resource to its cloud identifier with a specified {@code
   * resolveOption}.
   */
  public String resolve(BqResolvedOptions resolveOption) {
    switch (resolveOption) {
      case FULL_PATH:
        return projectId
            + BqResolvedOptions.BQ_PROJECT_DELIMITER
            + datasetId
            + BqResolvedOptions.BQ_TABLE_DELIMITER
            + dataTableId;
      case FULL_PATH_SQL:
        return projectId
            + BqResolvedOptions.BQ_PROJECT_DELIMITER_SQL
            + datasetId
            + BqResolvedOptions.BQ_TABLE_DELIMITER
            + dataTableId;
      case TABLE_ID_ONLY:
        return dataTableId;
      case DATASET_ID_ONLY:
        return datasetId;
      case PROJECT_ID_ONLY:
        return projectId;
      default:
        throw new IllegalArgumentException("Unknown BigQuery data table resolve option.");
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

  public String getDataTableId() {
    return dataTableId;
  }
}
