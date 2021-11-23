package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDBqDataTable;
import bio.terra.cli.serialization.userfacing.input.CreateBqDataTableParams;
import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataTable;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpBigQueryDataTableResource;
import bio.terra.workspace.model.ResourceDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a BigQuery data table workspace resource. Instances of this class are
 * part of the current context or state.
 */
public class BqDataTable extends Resource {

  private static final Logger logger = LoggerFactory.getLogger(BqDataTable.class);

  private String projectId;
  private String datasetId;
  private String dataTableId;

  /**
   * Delimiter between the project id, dataset id and data table id for a BigQuery DataTable.
   *
   * <p>The choice is somewhat arbitrary. BigQuery tables do not have true URIs. The '.' delimiter
   * allows the path to be used directly in SQL calls with a BigQuery extension.
   */
  private static final char BQ_PROJECT_DATA_TABLE_DELIMITER = '.';

  /** Deserialize an instance of the disk format to the internal object. */
  public BqDataTable(PDBqDataTable configFromDisk) {
    super(configFromDisk);
    this.projectId = configFromDisk.projectId;
    this.datasetId = configFromDisk.datasetId;
    this.dataTableId = configFromDisk.dataTableId;
  }

  /** Deserialize an instance of the WSM client library object to the internal object. */
  public BqDataTable(ResourceDescription wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.BQ_DATA_TABLE;
    this.projectId = wsmObject.getResourceAttributes().getGcpBqDataTable().getProjectId();
    this.datasetId = wsmObject.getResourceAttributes().getGcpBqDataTable().getDatasetId();
    this.dataTableId = wsmObject.getResourceAttributes().getGcpBqDataTable().getDataTableId();
  }

  /** Deserialize an instance of the WSM client library create object to the internal object. */
  public BqDataTable(GcpBigQueryDataTableResource wsmObject) {
    super(wsmObject.getMetadata());
    this.resourceType = Type.BQ_DATA_TABLE;
    this.projectId = wsmObject.getAttributes().getProjectId();
    this.datasetId = wsmObject.getAttributes().getDatasetId();
    this.dataTableId = wsmObject.getAttributes().getDataTableId();
  }

  /**
   * Serialize the internal representation of the resource to the format for command input/output.
   */
  public UFBqDataTable serializeToCommand() {
    return new UFBqDataTable(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public PDBqDataTable serializeToDisk() {
    return new PDBqDataTable(this);
  }

  /**
   * Add a BigQuery data table as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static BqDataTable addReferenced(CreateBqDataTableParams createParams) {
    validateEnvironmentVariableName(createParams.resourceFields.name);

    // call WSM to add the reference. use the pet SA credentials instead of the end user's
    // credentials, because they include the cloud-platform scope. WSM needs the cloud-platform
    // scope to perform its access check before adding the reference. note that this means a user
    // cannot add a reference unless their pet SA has access to it.
    GcpBigQueryDataTableResource addedResource =
        WorkspaceManagerService.fromContextForPetSa()
            .createReferencedBigQueryDataTable(Context.requireWorkspace().getId(), createParams);
    logger.info("Created BQ data table: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new BqDataTable(addedResource);
  }

  /** Update a BigQuery data table referenced resource in the workspace. */
  public void updateReferenced(UpdateResourceParams updateParams) {
    if (updateParams.name != null) {
      validateEnvironmentVariableName(updateParams.name);
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedBigQueryDataTable(Context.requireWorkspace().getId(), id, updateParams);
    super.updatePropertiesAndSync(updateParams);
  }

  /** Delete a BigQuery data table referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedBigQueryDataTable(Context.requireWorkspace().getId(), id);
  }

  /** Delete a BigQuery data table controlled resource in the workspace. */
  protected void deleteControlled() {
    throw new UnsupportedOperationException(
        "Unsupport this operation because workspace manager does not support creating a controlled data table");
  }

  /**
   * Resolve a BigQuery data table resource to its cloud identifier. Returns the SQL path to the
   * data table: [GCP project id].[BQ dataset id].[BQ data table id]
   */
  public String resolve() {
    return projectId
        + BQ_PROJECT_DATA_TABLE_DELIMITER
        + datasetId
        + BQ_PROJECT_DATA_TABLE_DELIMITER
        + dataTableId;
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
