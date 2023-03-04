package bio.terra.cli.businessobject.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.serialization.persisted.resource.PDBqDataset;
import bio.terra.cli.serialization.userfacing.input.CreateBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedBqDatasetParams;
import bio.terra.cli.serialization.userfacing.resource.UFBqDataset;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import bio.terra.workspace.model.GcpBigQueryDatasetResource;
import bio.terra.workspace.model.ResourceDescription;
import com.google.api.services.bigquery.model.Dataset;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal representation of a BigQuery dataset workspace resource. Instances of this class are
 * part of the current context or state.
 */
public class BqDataset extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(BqDataset.class);
  /**
   * Delimiter between the project id and dataset id for a BigQuery dataset.
   *
   * <p>The choice is somewhat arbitrary. BigQuery Datatsets do not have true URIs. The '.'
   * delimiter allows the path to be used directly in SQL calls with a BigQuery extension.
   */
  private static final char BQ_PROJECT_DATASET_DELIMITER = '.';

  private String projectId;
  private String datasetId;

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
   * Add a BigQuery dataset as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static BqDataset addReferenced(CreateBqDatasetParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    GcpBigQueryDatasetResource addedResource =
        WorkspaceManagerService.fromContext()
            .createReferencedBigQueryDataset(Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created BQ dataset: {}", addedResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResources();
    return new BqDataset(addedResource);
  }

  /**
   * Create a BigQuery dataset as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static BqDataset createControlled(CreateBqDatasetParams createParams) {
    validateResourceName(createParams.resourceFields.name);

    // call WSM to create the resource
    GcpBigQueryDatasetResource createdResource =
        WorkspaceManagerService.fromContext()
            .createControlledBigQueryDataset(Context.requireWorkspace().getUuid(), createParams);
    logger.info("Created BQ dataset: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResources();
    return new BqDataset(createdResource);
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

  /** Update a BigQuery dataset referenced resource in the workspace. */
  public void updateReferenced(UpdateReferencedBqDatasetParams updateParams) {
    if (updateParams.resourceParams.name != null) {
      validateResourceName(updateParams.resourceParams.name);
    }
    if (updateParams.projectId != null) {
      this.projectId = updateParams.projectId;
    }
    if (updateParams.datasetId != null) {
      this.datasetId = updateParams.datasetId;
    }
    if (updateParams.cloningInstructions != null) {
      this.cloningInstructions = updateParams.cloningInstructions;
    }
    WorkspaceManagerService.fromContext()
        .updateReferencedBigQueryDataset(Context.requireWorkspace().getUuid(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceParams);
  }

  /** Update a BigQuery dataset controlled resource in the workspace. */
  public void updateControlled(UpdateControlledBqDatasetParams updateParams) {
    if (updateParams.resourceFields.name != null) {
      validateResourceName(updateParams.resourceFields.name);
    }
    WorkspaceManagerService.fromContext()
        .updateControlledBigQueryDataset(Context.requireWorkspace().getUuid(), id, updateParams);
    super.updatePropertiesAndSync(updateParams.resourceFields);
  }

  /** Delete a BigQuery dataset referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    WorkspaceManagerService.fromContext()
        .deleteReferencedBigQueryDataset(Context.requireWorkspace().getUuid(), id);
  }

  /** Delete a BigQuery dataset controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    WorkspaceManagerService.fromContext()
        .deleteControlledBigQueryDataset(Context.requireWorkspace().getUuid(), id);
  }

  /**
   * Resolve a BigQuery dataset resource to its cloud identifier. Returns the SQL path to the
   * dataset: [GCP project id].[BQ dataset id]
   */
  public String resolve() {
    return resolve(BqResolvedOptions.FULL_PATH);
  }

  /**
   * Resolve a BigQuery dataset resource to its cloud identifier. Returns the SQL path to the
   * dataset: [GCP project id].[BQ dataset id]
   */
  public String resolve(BqResolvedOptions resolveOption) {
    return switch (resolveOption) {
      case FULL_PATH -> projectId + BQ_PROJECT_DATASET_DELIMITER + datasetId;
      case DATASET_ID_ONLY -> datasetId;
      case PROJECT_ID_ONLY -> projectId;
      default -> throw new IllegalArgumentException("Unknown BigQuery dataset resolve option.");
    };
  }

  /** Query the cloud for information about the dataset. */
  public Optional<Dataset> getDataset() {
    try {
      BigQueryCow bigQueryCow =
          CrlUtils.createBigQueryCow(Context.requireUser().getPetSACredentials());
      return Optional.of(bigQueryCow.datasets().get(projectId, datasetId).execute());
    } catch (Exception ex) {
      logger.error("Caught exception looking up dataset", ex);
      return Optional.empty();
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
