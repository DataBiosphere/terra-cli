package bio.terra.cli.service;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.GoogleBigQuery;
import bio.terra.cli.service.utils.GoogleCloudStorage;
import bio.terra.cli.service.utils.WorkspaceManagerService;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.GcpBigQueryDatasetAttributes;
import bio.terra.workspace.model.GcpBigQueryDatasetResource;
import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import bio.terra.workspace.model.GcpGcsBucketLifecycleRule;
import bio.terra.workspace.model.GcpGcsBucketResource;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceList;
import bio.terra.workspace.model.RoleBindingList;
import bio.terra.workspace.model.StewardshipType;
import bio.terra.workspace.model.WorkspaceDescription;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class manipulates the workspace properties of the workspace context object. */
public class WorkspaceManager {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManager.class);

  private final GlobalContext globalContext;
  private final WorkspaceContext workspaceContext;

  public WorkspaceManager(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    this.globalContext = globalContext;
    this.workspaceContext = workspaceContext;
  }

  // ====================================================
  // Workspaces

  /**
   * List all workspaces that a user has read access to.
   *
   * @param offset the offset to use when listing workspaces (zero to start from the beginning)
   * @param limit the maximum number of workspaces to return
   * @return list of workspaces
   */
  public List<WorkspaceDescription> listWorkspaces(int offset, int limit) {
    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // fetch the list of workspaces from WSM
    return new WorkspaceManagerService(globalContext.server, currentUser)
        .listWorkspaces(offset, limit)
        .getWorkspaces();
  }

  /**
   * Create a new workspace.
   *
   * @param displayName optional display name
   * @param description optional description
   */
  public void createWorkspace(String displayName, String description) {
    // check that there is no existing workspace already mounted
    if (!workspaceContext.isEmpty()) {
      throw new UserActionableException("There is already a workspace mounted to this directory.");
    }

    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call WSM to create the workspace object and backing Google context
    WorkspaceDescription createdWorkspace =
        new WorkspaceManagerService(globalContext.server, currentUser)
            .createWorkspace(displayName, description);
    logger.info("Created workspace: id={}, {}", createdWorkspace.getId(), createdWorkspace);

    // update the workspace context with the current workspace
    // note that this state is persisted to disk. it will be useful for code called in the same or a
    // later CLI command/process
    workspaceContext.updateWorkspace(createdWorkspace);
  }

  /**
   * Fetch an existing workspace and mount it to the current directory.
   *
   * @throws UserActionableException if there is already a different workspace mounted to the
   *     current directory
   */
  public void mountWorkspace(String workspaceId) {
    // check that the workspace id is a valid UUID
    UUID workspaceIdParsed = UUID.fromString(workspaceId);

    // check that either there is no workspace currently mounted, or its id matches this one
    if (!(workspaceContext.isEmpty()
        || workspaceContext.getWorkspaceId().equals(workspaceIdParsed))) {
      throw new UserActionableException(
          "There is already a different workspace mounted to this directory.");
    }

    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call WSM to fetch the existing workspace object and backing Google context
    WorkspaceDescription existingWorkspace =
        new WorkspaceManagerService(globalContext.server, currentUser)
            .getWorkspace(workspaceIdParsed);
    logger.info("Existing workspace: id={}, {}", existingWorkspace.getId(), existingWorkspace);

    // update the workspace context with the current workspace
    // note that this state is persisted to disk. it will be useful for code called in the same or a
    // later CLI command/process
    workspaceContext.updateWorkspace(existingWorkspace);
  }

  /**
   * Delete the workspace that is mounted to the current directory.
   *
   * @return the deleted workspace id
   * @throws UserActionableException if there is no workspace currently mounted
   */
  public UUID deleteWorkspace() {
    // check that there is a workspace currently mounted
    workspaceContext.requireCurrentWorkspace();

    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call WSM to delete the existing workspace object
    WorkspaceDescription workspace = workspaceContext.terraWorkspaceModel;
    new WorkspaceManagerService(globalContext.server, currentUser)
        .deleteWorkspace(workspaceContext.getWorkspaceId());
    logger.info("Deleted workspace: id={}, {}", workspace.getId(), workspace);

    // unset the workspace in the current context
    // note that this state is persisted to disk. it will be useful for code called in the same or a
    // later CLI command/process
    workspaceContext.deleteWorkspace();

    return workspace.getId();
  }

  /**
   * Update the mutable properties of the workspace that is mounted to the current directory.
   *
   * @param displayName optional display name
   * @param description optional description
   * @throws UserActionableException if there is no workspace currently mounted
   */
  public void updateWorkspace(String displayName, String description) {
    // check that there is a workspace currently mounted
    workspaceContext.requireCurrentWorkspace();

    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call WSM to update the existing workspace object
    WorkspaceDescription workspace = workspaceContext.terraWorkspaceModel;
    WorkspaceDescription updatedWorkspace =
        new WorkspaceManagerService(globalContext.server, currentUser)
            .updateWorkspace(workspaceContext.getWorkspaceId(), displayName, description);
    logger.info("Updated workspace: id={}, {}", workspace.getId(), workspace);

    // update the workspace in the current context
    // note that this state is persisted to disk. it will be useful for code called in the same or a
    // later CLI command/process
    workspaceContext.updateWorkspace(updatedWorkspace);
  }

  // ====================================================
  // Workspace users

  /**
   * Add a user to the workspace that is mounted to the current directory. Possible roles are
   * defined by the WSM client library.
   *
   * @param userEmail the user to add
   * @param iamRole the role to assign the user
   * @throws UserActionableException if there is no workspace currently mounted
   */
  public void addUserToWorkspace(String userEmail, IamRole iamRole) {
    // check that there is a workspace currently mounted
    workspaceContext.requireCurrentWorkspace();

    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call WSM to add a user + role to the existing workspace
    new WorkspaceManagerService(globalContext.server, currentUser)
        .grantIamRole(workspaceContext.getWorkspaceId(), userEmail, iamRole);
    logger.info(
        "Added user to workspace: id={}, user={}, role={}",
        workspaceContext.getWorkspaceId(),
        userEmail,
        iamRole);
  }

  /**
   * Remove a user + role from the workspace that is mounted to the current directory. Possible
   * roles are defined by the WSM client library.
   *
   * @param userEmail the user to remove
   * @param iamRole the role to remove from the user
   * @throws UserActionableException if there is no workspace currently mounted
   */
  public void removeUserFromWorkspace(String userEmail, IamRole iamRole) {
    // check that there is a workspace currently mounted
    workspaceContext.requireCurrentWorkspace();

    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call WSM to remove a user + role from the existing workspace
    new WorkspaceManagerService(globalContext.server, currentUser)
        .removeIamRole(workspaceContext.getWorkspaceId(), userEmail, iamRole);
    logger.info(
        "Removed user from workspace: id={}, user={}, role={}",
        workspaceContext.getWorkspaceId(),
        userEmail,
        iamRole);
  }

  /**
   * List the roles in a workspace and all the users that have each role.
   *
   * @return a map of roles to the list of users that have that role
   */
  public RoleBindingList listUsersOfWorkspace() {
    // check that there is a workspace currently mounted
    workspaceContext.requireCurrentWorkspace();

    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call WSM to get the users + roles for the existing workspace
    return new WorkspaceManagerService(globalContext.server, currentUser)
        .getRoles(workspaceContext.getWorkspaceId());
  }

  // ====================================================
  // Workspace resources, controlled & referenced

  /**
   * Update the cached list of resources (controlled & referenced) in the workspace, by fetching the
   * up-to-date list from WSM.
   */
  private void updateResourcesCache() {
    // check that there is a workspace currently mounted
    workspaceContext.requireCurrentWorkspace();

    // check that there is a current user, we will use their credentials to communicate with WSM
    TerraUser currentUser = globalContext.requireCurrentTerraUser();

    // call WSM to get the list of resources for the existing workspace
    // TODO: keep calling the enumerate endpoint until no results are returned
    ResourceList resourceList =
        new WorkspaceManagerService(globalContext.server, currentUser)
            .enumerateResources(workspaceContext.getWorkspaceId(), 0, 100, null, null);

    // update the cache with the list of resources fetched from WSM
    workspaceContext.setResources(resourceList.getResources());
  }

  /**
   * List all the resources in the workspace. Also updates the cached list of resources.
   *
   * @return a list of resources in the workspace
   */
  public List<ResourceDescription> listResources() {
    updateResourcesCache();

    return new ArrayList<>(workspaceContext.resources.values());
  }

  /**
   * Get an existing resource in the workspace. Also updates the cached list of resources.
   *
   * @param name name of resource
   * @return the resource description object
   */
  public ResourceDescription getResource(String name) {
    updateResourcesCache();

    ResourceDescription resource = workspaceContext.getResource(name);
    if (resource == null) {
      throw new UserActionableException(name + " not found.");
    }
    return resource;
  }

  /**
   * Add a GCS bucket as a referenced resource in the workspace. Also updates the cached list of
   * resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @param description description of the resource
   * @param cloningInstructions instructions for how to handle the resource when cloning the
   *     workspace
   * @param gcsBucketName GCS bucket name
   * @return the resource description object that was created
   */
  public ResourceDescription createReferencedGcsBucket(
      String name,
      String description,
      CloningInstructionsEnum cloningInstructions,
      String gcsBucketName) {
    return createResource(
        name,
        (currentUser) -> {
          GcpGcsBucketResource gcsBucketResource =
              new WorkspaceManagerService(globalContext.server, currentUser)
                  .createReferencedGcsBucket(
                      workspaceContext.getWorkspaceId(),
                      name,
                      description,
                      cloningInstructions,
                      gcsBucketName);
          logger.info("Created new GCS bucket REFERENCED resource: {}", gcsBucketResource);
        });
  }

  /**
   * Add a Big Query dataset as a referenced resource in the workspace. Also updates the cached list
   * of resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @param description description of the resource
   * @param cloningInstructions instructions for how to handle the resource when cloning the
   *     workspace
   * @param googleProjectId Google project id where the Big Query dataset resides
   * @param bigQueryDatasetId Big Query dataset id
   * @return the resource description object that was created
   */
  public ResourceDescription createReferencedBigQueryDataset(
      String name,
      String description,
      CloningInstructionsEnum cloningInstructions,
      String googleProjectId,
      String bigQueryDatasetId) {
    return createResource(
        name,
        (currentUser) -> {
          GcpBigQueryDatasetResource bigQueryDatasetResource =
              new WorkspaceManagerService(globalContext.server, currentUser)
                  .createReferencedBigQueryDataset(
                      workspaceContext.getWorkspaceId(),
                      name,
                      description,
                      cloningInstructions,
                      googleProjectId,
                      bigQueryDatasetId);
          logger.info(
              "Created new Big Query dataset REFERENCED resource: {}", bigQueryDatasetResource);
        });
  }

  /**
   * Add a GCS bucket as a controlled resource in the workspace. Also updates the cached list of
   * resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @param description description of the resource
   * @param cloningInstructions instructions for how to handle the resource when cloning the
   *     workspace
   * @param accessScope access to allow other workspaces users
   * @param gcsBucketName GCS bucket name (https://cloud.google.com/storage/docs/naming-buckets)
   * @param defaultStorageClass GCS storage class
   *     (https://cloud.google.com/storage/docs/storage-classes)
   * @param lifecycleRules list of lifecycle rules for the bucket
   *     (https://cloud.google.com/storage/docs/lifecycle)
   * @param location GCS bucket location (https://cloud.google.com/storage/docs/locations)
   * @return the resource description object that was created
   */
  public ResourceDescription createControlledGcsBucket(
      String name,
      String description,
      CloningInstructionsEnum cloningInstructions,
      AccessScope accessScope,
      String gcsBucketName,
      @Nullable GcpGcsBucketDefaultStorageClass defaultStorageClass,
      List<GcpGcsBucketLifecycleRule> lifecycleRules,
      @Nullable String location) {
    return createResource(
        name,
        (currentUser) -> {
          GcpGcsBucketResource gcsBucketResource =
              new WorkspaceManagerService(globalContext.server, currentUser)
                  .createControlledGcsBucket(
                      workspaceContext.getWorkspaceId(),
                      name,
                      description,
                      cloningInstructions,
                      accessScope,
                      gcsBucketName,
                      defaultStorageClass,
                      lifecycleRules,
                      location);
          logger.info("Created new GCS bucket CONTROLLED resource: {}", gcsBucketResource);
        });
  }

  /**
   * Add a Big Query dataset as a controlled resource in the workspace. Also updates the cached list
   * of resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @param description description of the resource
   * @param cloningInstructions instructions for how to handle the resource when cloning the
   *     workspace
   * @param accessScope access to allow other workspaces users
   * @param bigQueryDatasetId Big Query dataset id
   *     (https://cloud.google.com/bigquery/docs/datasets#dataset-naming)
   * @param location Big Query dataset location (https://cloud.google.com/bigquery/docs/locations)
   * @return the resource description object that was created
   */
  public ResourceDescription createControlledBigQueryDataset(
      String name,
      String description,
      CloningInstructionsEnum cloningInstructions,
      AccessScope accessScope,
      String bigQueryDatasetId,
      @Nullable String location) {
    return createResource(
        name,
        (currentUser) -> {
          GcpBigQueryDatasetResource bigQueryDatasetResource =
              new WorkspaceManagerService(globalContext.server, currentUser)
                  .createControlledBigQueryDataset(
                      workspaceContext.getWorkspaceId(),
                      name,
                      description,
                      cloningInstructions,
                      accessScope,
                      bigQueryDatasetId,
                      location);
          logger.info(
              "Created new Big Query dataset CONTROLLED resource: {}", bigQueryDatasetResource);
        });
  }

  /**
   * Create a new resource in the workspace. Also updates the cached list of resources.
   *
   * @param resourceName name of resource to create
   * @return the resource description object that was created
   */
  private ResourceDescription createResource(
      String resourceName, Consumer<TerraUser> createResource) {
    workspaceContext.requireCurrentWorkspace();

    if (!isValidEnvironmentVariableName(resourceName)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // create the resource
    createResource.accept(globalContext.requireCurrentTerraUser());

    // persist the cloud resource locally
    updateResourcesCache();

    // return just the summary object
    return workspaceContext.getResource(resourceName);
  }

  /**
   * Check if the name only contains alphanumeric and underscore characters.
   *
   * @param name string to check
   * @return true if the string is a valid environment variable name
   */
  private static boolean isValidEnvironmentVariableName(String name) {
    return !Pattern.compile("[^a-zA-Z0-9_]").matcher(name).find();
  }

  /**
   * Delete a GCS bucket referenced resource in the workspace. Also updates the cached list of
   * resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @return the resource description object that was deleted
   */
  public ResourceDescription deleteReferencedGcsBucket(String name) {
    return deleteResource(
        name,
        StewardshipType.REFERENCED,
        (currentUser, resourceId) -> {
          new WorkspaceManagerService(globalContext.server, currentUser)
              .deleteReferencedGcsBucket(workspaceContext.getWorkspaceId(), resourceId);
        });
  }

  /**
   * Delete a Big Query dataset referenced resource in the workspace. Also updates the cached list
   * of resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @return the resource description object that was deleted
   */
  public ResourceDescription deleteReferencedBigQueryDataset(String name) {
    return deleteResource(
        name,
        StewardshipType.REFERENCED,
        (currentUser, resourceId) -> {
          new WorkspaceManagerService(globalContext.server, currentUser)
              .deleteReferencedBigQueryDataset(workspaceContext.getWorkspaceId(), resourceId);
        });
  }

  /**
   * Delete a GCS bucket controlled resource in the workspace. Also updates the cached list of
   * resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @return the resource description object that was deleted
   */
  public ResourceDescription deleteControlledGcsBucket(String name) {
    return deleteResource(
        name,
        StewardshipType.CONTROLLED,
        (currentUser, resourceId) -> {
          new WorkspaceManagerService(globalContext.server, currentUser)
              .deleteControlledGcsBucket(workspaceContext.getWorkspaceId(), resourceId);
        });
  }

  /**
   * Delete a Big Query dataset controlled resource in the workspace. Also updates the cached list
   * of resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @return the resource description object that was deleted
   */
  public ResourceDescription deleteControlledBigQueryDataset(String name) {
    return deleteResource(
        name,
        StewardshipType.CONTROLLED,
        (currentUser, resourceId) -> {
          new WorkspaceManagerService(globalContext.server, currentUser)
              .deleteControlledBigQueryDataset(workspaceContext.getWorkspaceId(), resourceId);
        });
  }

  /**
   * Delete an existing resource in the workspace. Also updates the cached list of resources.
   *
   * <p>This method throws a UserActionableException if the stewardship type does not match, to
   * point the user towards using a different command.
   *
   * @param resourceName name of resource to delete
   * @param stewardshipType expected stewardship type of the resource to delete
   * @return the resource description object that was deleted
   */
  private ResourceDescription deleteResource(
      String resourceName,
      StewardshipType stewardshipType,
      BiConsumer<TerraUser, UUID> deleteResource) {
    workspaceContext.requireCurrentWorkspace();

    // get the summary object
    ResourceDescription resourceToDelete = workspaceContext.getResource(resourceName);

    // check if the resource is the wrong stewardship type
    if (!resourceToDelete.getMetadata().getStewardshipType().equals(stewardshipType)) {
      throw new UserActionableException(
          "A resource with this name exists in the workspace, but it is "
              + resourceToDelete.getMetadata().getStewardshipType());
    }

    // delete the resource
    deleteResource.accept(
        globalContext.requireCurrentTerraUser(), resourceToDelete.getMetadata().getResourceId());

    // persist the cloud resource locally
    updateResourcesCache();

    // return just the summary object
    return resourceToDelete;
  }

  /**
   * Check whether a user can access the referenced GCS bucket, either with their end-user or pet SA
   * credentials.
   *
   * @param resource resource to check access for
   * @param usePetSa true to check access using the pet SA credentials, false to check access using
   *     the end-user credentials
   * @return true if the user can access the referenced GCS bucket
   */
  public boolean checkAccessToReferencedGcsBucket(ResourceDescription resource, boolean usePetSa) {
    if (!resource.getMetadata().getStewardshipType().equals(StewardshipType.REFERENCED)) {
      throw new UserActionableException(
          "Unexpected stewardship type. Checking access is intended for REFERENCED resources only.");
    }

    // TODO: replace this with a call to WSM once the endpoint is available (PF-702)
    TerraUser currentUser = globalContext.requireCurrentTerraUser();
    GoogleCredentials credentials =
        usePetSa ? currentUser.petSACredentials : currentUser.userCredentials;

    return new GoogleCloudStorage(credentials, workspaceContext.getGoogleProject())
        .checkObjectsListAccess(getGcsBucketUrl(resource));
  }

  /**
   * Check whether a user can access the referenced Big Query dataset, either with their end-user or
   * pet SA credentials.
   *
   * @param resource resource to check access for
   * @param usePetSa true to check access using the pet SA credentials, false to check access using
   *     the end-user credentials
   * @return true if the user can access the referenced Big Query dataset
   */
  public boolean checkAccessToReferencedBigQueryDataset(
      ResourceDescription resource, boolean usePetSa) {
    if (!resource.getMetadata().getStewardshipType().equals(StewardshipType.REFERENCED)) {
      throw new UserActionableException(
          "Unexpected stewardship type. Checking access is intended for REFERENCED resources only.");
    }

    // TODO: replace this with a call to WSM once the endpoint is available (PF-702)
    TerraUser currentUser = globalContext.requireCurrentTerraUser();
    GoogleCredentials credentials =
        usePetSa ? currentUser.petSACredentials : currentUser.userCredentials;

    GcpBigQueryDatasetAttributes gcpBigQueryDatasetAttributes =
        resource.getResourceAttributes().getGcpBqDataset();
    return new GoogleBigQuery(credentials, workspaceContext.getGoogleProject())
        .checkListTablesAccess(
            DatasetId.of(
                gcpBigQueryDatasetAttributes.getProjectId(),
                gcpBigQueryDatasetAttributes.getDatasetId()));
  }

  /**
   * Delimiter between the project id and dataset id for a Big Query dataset.
   *
   * <p>The choice is somewhat arbitrary. BigQuery Datatsets do not have true URIs. The '.'
   * delimiter allows the string to be used directly in SQL calls with a Big Query extension.
   */
  private static final String GCS_BUCKET_URL_PREFIX = "gs://";

  /**
   * Delimiter between the project id and dataset id for a Big Query dataset.
   *
   * <p>The choice is somewhat arbitrary. BigQuery Datatsets do not have true URIs. The '.'
   * delimiter allows the string to be used directly in SQL calls with a Big Query extension.
   */
  private static final char BQ_PROJECT_DATASET_DELIMITER = '.';

  /**
   * Utility method for getting the full URL to a GCS bucket, including the 'gs://' prefix.
   *
   * @param resource GCS bucket resource
   * @return full URL to the bucket
   */
  public static String getGcsBucketUrl(ResourceDescription resource) {
    return GCS_BUCKET_URL_PREFIX
        + resource.getResourceAttributes().getGcpGcsBucket().getBucketName();
  }

  /**
   * Utility method for getting the full path to a Big Query dataset: [GCP project id].[BQ dataset
   * id]
   *
   * @param resource Big Query dataset resource
   * @return full path to the dataset
   */
  public static String getBigQueryDatasetPath(ResourceDescription resource) {
    GcpBigQueryDatasetAttributes datasetAttributes =
        resource.getResourceAttributes().getGcpBqDataset();
    return datasetAttributes.getProjectId()
        + BQ_PROJECT_DATASET_DELIMITER
        + datasetAttributes.getDatasetId();
  }
}
