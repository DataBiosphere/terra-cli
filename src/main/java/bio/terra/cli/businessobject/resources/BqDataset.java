package bio.terra.cli.businessobject.resources;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.command.createupdate.CreateUpdateBqDataset;
import bio.terra.cli.serialization.command.resources.CommandBqDataset;
import bio.terra.cli.serialization.persisted.resources.DiskBqDataset;
import bio.terra.cli.service.GoogleBigQuery;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpBigQueryDatasetResource;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.StewardshipType;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;
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
  public BqDataset(DiskBqDataset configFromDisk) {
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
  public CommandBqDataset serializeToCommand() {
    return new CommandBqDataset(this);
  }

  /** Serialize the internal representation of the resource to the format for writing to disk. */
  public DiskBqDataset serializeToDisk() {
    return new DiskBqDataset(this);
  }

  /**
   * Add a Big Query dataset as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static BqDataset addReferenced(CreateUpdateBqDataset createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.resourceFields.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to add the reference
    GcpBigQueryDatasetResource addedResource =
        new WorkspaceManagerService()
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
  public static BqDataset createControlled(CreateUpdateBqDataset createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.resourceFields.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to create the resource
    GcpBigQueryDatasetResource createdResource =
        new WorkspaceManagerService()
            .createControlledBigQueryDataset(Context.requireWorkspace().getId(), createParams);
    logger.info("Created BQ dataset: {}", createdResource);

    // convert the WSM object to a CLI object
    Context.requireWorkspace().listResourcesAndSync();
    return new BqDataset(createdResource);
  }

  /** Delete a Big Query dataset referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    new WorkspaceManagerService()
        .deleteReferencedBigQueryDataset(Context.requireWorkspace().getId(), id);
  }

  /** Delete a Big Query dataset controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    new WorkspaceManagerService()
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

  /**
   * Check whether a user can access the Big Query dataset resource.
   *
   * @param credentialsToUse enum value indicates whether to use end-user or pet SA credentials for
   *     checking access
   * @return true if the user can access the referenced Big Query dataset with the given credentials
   * @throws UserActionableException if the resource is CONTROLLED
   */
  public boolean checkAccess(CheckAccessCredentials credentialsToUse) {
    if (!stewardshipType.equals(StewardshipType.REFERENCED)) {
      throw new UserActionableException(
          "Unexpected stewardship type. Checking access is intended for REFERENCED resources only.");
    }

    // TODO (PF-717): replace this with a call to WSM once an endpoint is available
    User currentUser = Context.requireUser();
    GoogleCredentials credentials =
        credentialsToUse.equals(CheckAccessCredentials.USER)
            ? currentUser.getUserCredentials()
            : currentUser.getPetSACredentials();

    return new GoogleBigQuery(credentials, Context.requireWorkspace().getGoogleProjectId())
        .checkListTablesAccess(DatasetId.of(projectId, datasetId));
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
