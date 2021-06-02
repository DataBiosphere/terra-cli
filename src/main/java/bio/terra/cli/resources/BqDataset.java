package bio.terra.cli.resources;

import bio.terra.cli.Context;
import bio.terra.cli.Resource;
import bio.terra.cli.User;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.command.createupdate.CreateUpdateBqDataset;
import bio.terra.cli.serialization.disk.resources.DiskBqDataset;
import bio.terra.cli.service.GoogleBigQuery;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpBigQueryDatasetResource;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.StewardshipType;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  // prefix for GCS bucket to make a valid URL.
  private static final String GCS_BUCKET_URL_PREFIX = "gs://";

  protected BqDataset(Builder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
  }

  /**
   * Add a GCS bucket as a referenced resource in the workspace.
   *
   * @return the resource that was added
   */
  public static BqDataset addReferenced(CreateUpdateBqDataset createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to add the reference
    GcpBigQueryDatasetResource addedResource =
        new WorkspaceManagerService()
            .createReferencedBigQueryDataset(Context.requireWorkspace().getId(), createParams);
    logger.info("Created BQ dataset: {}", addedResource);

    // convert the WSM object to a CLI object
    listAndSync();
    return new Builder(addedResource).build();
  }

  /**
   * Create a GCS bucket as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  public static BqDataset createControlled(CreateUpdateBqDataset createParams) {
    if (!Resource.isValidEnvironmentVariableName(createParams.name)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // call WSM to add the reference
    GcpBigQueryDatasetResource createdResource =
        new WorkspaceManagerService()
            .createControlledBigQueryDataset(Context.requireWorkspace().getId(), createParams);
    logger.info("Created BQ dataset: {}", createdResource);

    // convert the WSM object to a CLI object
    listAndSync();
    return new Builder(createdResource).build();
  }

  /** Delete a GCS bucket referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    new WorkspaceManagerService()
        .deleteReferencedBigQueryDataset(Context.requireWorkspace().getId(), id);
  }

  /** Delete a GCS bucket controlled resource in the workspace. */
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

  /**
   * Builder class to help construct an immutable Resource object with lots of properties.
   * Sub-classes extend this with resource type-specific properties.
   */
  public static class Builder extends Resource.Builder {
    private String projectId;
    private String datasetId;

    public Builder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public Builder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    /** Method that returns the resource type. Should be hard-coded in sub-classes. */
    public ResourceType getResourceType() {
      return ResourceType.BQ_DATASET;
    }

    /** Call the sub-class constructor. */
    public BqDataset build() {
      return new BqDataset(this);
    }

    /**
     * Populate this Builder object with properties from the WSM ResourceDescription object. This
     * method handles the metadata fields that apply to GCS buckets only.
     */
    public Builder(ResourceDescription wsmObject) {
      super(wsmObject.getMetadata());
      this.projectId = wsmObject.getResourceAttributes().getGcpBqDataset().getProjectId();
      this.datasetId = wsmObject.getResourceAttributes().getGcpBqDataset().getDatasetId();
    }

    /** Populate this Builder object with properties from the WSM GcpGcsBucketResource object. */
    public Builder(GcpBigQueryDatasetResource wsmObject) {
      super(wsmObject.getMetadata());
      this.projectId = wsmObject.getAttributes().getProjectId();
      this.datasetId = wsmObject.getAttributes().getDatasetId();
    }

    /**
     * Populate this Builder object with properties from the on-disk object. This method handles the
     * fields that apply to all resource types.
     */
    public Builder(DiskBqDataset configFromDisk) {
      super(configFromDisk);
      this.projectId = configFromDisk.projectId;
      this.datasetId = configFromDisk.datasetId;
    }
  }
}
