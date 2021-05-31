package bio.terra.cli.context.resources;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Resource;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.utils.Printer;
import bio.terra.cli.service.GoogleBigQuery;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.workspace.model.GcpBigQueryDatasetResource;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceType;
import bio.terra.workspace.model.StewardshipType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JsonDeserialize(builder = BqDataset.BqDatasetBuilder.class)
public class BqDataset extends Resource {
  private static final Logger logger = LoggerFactory.getLogger(BqDataset.class);

  public final String projectId;
  public final String datasetId;
  public final String location;

  /**
   * Delimiter between the project id and dataset id for a Big Query dataset.
   *
   * <p>The choice is somewhat arbitrary. BigQuery Datatsets do not have true URIs. The '.'
   * delimiter allows the path to be used directly in SQL calls with a Big Query extension.
   */
  private static final char BQ_PROJECT_DATASET_DELIMITER = '.';

  public BqDataset(BqDatasetBuilder builder) {
    super(builder);
    this.projectId = builder.projectId;
    this.datasetId = builder.datasetId;
    this.location = builder.location;
  }

  /**
   * Add a Big Query dataset as a referenced resource in the workspace.
   *
   * @return the resource that was created
   */
  protected BqDataset addReferenced() {
    // call WSM to add the reference
    GcpBigQueryDatasetResource createdResource =
        new WorkspaceManagerService()
            .createReferencedBigQueryDataset(
                GlobalContext.get().requireCurrentWorkspace().id, this);
    logger.info("Created Big Query dataset: {}", createdResource);

    // convert the WSM object to a CLI object
    return new BqDatasetBuilder(createdResource).build();
  }

  /**
   * Create a Big Query dataset as a controlled resource in the workspace.
   *
   * @return the resource that was created
   */
  protected BqDataset createControlled() {
    // call WSM to create the resource
    GcpBigQueryDatasetResource createdResource =
        new WorkspaceManagerService()
            .createControlledBigQueryDataset(
                GlobalContext.get().requireCurrentWorkspace().id, this);
    logger.info("Created Big Query dataset: {}", createdResource);

    // convert the WSM object to a CLI object
    return new BqDatasetBuilder(createdResource).build();
  }

  /** Delete a Big Query dataset referenced resource in the workspace. */
  protected void deleteReferenced() {
    // call WSM to delete the reference
    new WorkspaceManagerService()
        .deleteReferencedBigQueryDataset(
            GlobalContext.get().requireCurrentWorkspace().id, resourceId);
  }

  /** Delete a Big Query dataset controlled resource in the workspace. */
  protected void deleteControlled() {
    // call WSM to delete the resource
    new WorkspaceManagerService()
        .deleteControlledBigQueryDataset(
            GlobalContext.get().requireCurrentWorkspace().id, resourceId);
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
        throw new IllegalArgumentException("Unknown Big Query dataset resolve operation.");
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
    TerraUser currentUser = GlobalContext.get().requireCurrentTerraUser();
    GoogleCredentials credentials =
        credentialsToUse.equals(CheckAccessCredentials.USER)
            ? currentUser.userCredentials
            : currentUser.petSACredentials;

    return new GoogleBigQuery(
            credentials, GlobalContext.get().requireCurrentWorkspace().googleProjectId)
        .checkListTablesAccess(DatasetId.of(projectId, datasetId));
  }

  /** Print out a Big Query dataset resource in text format. */
  public void printText() {
    super.printText();
    PrintStream OUT = Printer.getOut();
    OUT.println("GCP project id: " + projectId);
    OUT.println("Big Query dataset id: " + datasetId);
  }

  /** Builder class to help construct an immutable BqDataset object with lots of properties. */
  @JsonPOJOBuilder(buildMethodName = "build", withPrefix = "")
  public static class BqDatasetBuilder extends ResourceBuilder {
    public String projectId;
    public String datasetId;
    public String location;

    public BqDatasetBuilder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public BqDatasetBuilder datasetId(String datasetId) {
      this.datasetId = datasetId;
      return this;
    }

    public BqDatasetBuilder location(String location) {
      this.location = location;
      return this;
    }

    /** Subclass-specific method that returns the resource type. */
    @JsonIgnore
    public ResourceType getResourceType() {
      return ResourceType.BIG_QUERY_DATASET;
    }

    /** Subclass-specific method that calls the sub-class constructor. */
    public BqDataset build() {
      return new BqDataset(this);
    }

    /** Default constructor for Jackson. */
    public BqDatasetBuilder() {
      super();
    }

    /**
     * Populate this Resource object with properties from the WSM ResourceDescription object. This
     * method handles the metadata fields that apply to Big Query datasets only.
     */
    public BqDatasetBuilder(ResourceDescription wsmObject) {
      super(wsmObject.getMetadata());
      this.projectId = wsmObject.getResourceAttributes().getGcpBqDataset().getProjectId();
      this.datasetId = wsmObject.getResourceAttributes().getGcpBqDataset().getDatasetId();
    }

    /**
     * Populate this Resource object with properties from the WSM GcpBigQueryDatasetResource object.
     */
    public BqDatasetBuilder(GcpBigQueryDatasetResource wsmObject) {
      super(wsmObject.getMetadata());
      this.projectId = wsmObject.getAttributes().getProjectId();
      this.datasetId = wsmObject.getAttributes().getDatasetId();
    }
  }
}
