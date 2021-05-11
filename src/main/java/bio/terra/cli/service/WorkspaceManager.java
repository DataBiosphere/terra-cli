package bio.terra.cli.service;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.context.resources.GcsBucketLifecycle;
import bio.terra.cli.service.utils.GoogleBigQuery;
import bio.terra.cli.service.utils.GoogleCloudStorage;
import bio.terra.cli.service.utils.WorkspaceManagerService;
import bio.terra.workspace.model.GcpAiNotebookInstanceAttributes;
import bio.terra.workspace.model.GcpAiNotebookInstanceCreationParameters;
import bio.terra.workspace.model.GcpBigQueryDatasetAttributes;
import bio.terra.workspace.model.GcpGcsBucketDefaultStorageClass;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.RoleBindingList;
import bio.terra.workspace.model.StewardshipType;
import bio.terra.workspace.model.WorkspaceDescription;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
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

    // call WSM to fetch the list of resources in the workspace
    updateResourcesCache();
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
    List<ResourceDescription> resourceList =
        new WorkspaceManagerService(globalContext.server, currentUser)
            .enumerateAllResources(
                workspaceContext.getWorkspaceId(), globalContext.resourcesCacheSize);

    // update the cache with the list of resources fetched from WSM
    workspaceContext.updateResources(resourceList);
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
   * @param resourceToAdd resource definition to add
   * @return the resource description object that was created
   */
  public ResourceDescription createReferencedGcsBucket(ResourceDescription resourceToAdd) {
    return createResource(
        resourceToAdd.getMetadata().getName(),
        () ->
            new WorkspaceManagerService(
                    globalContext.server, globalContext.requireCurrentTerraUser())
                .createReferencedGcsBucket(workspaceContext.getWorkspaceId(), resourceToAdd));
  }

  /**
   * Add a Big Query dataset as a referenced resource in the workspace. Also updates the cached list
   * of resources.
   *
   * @param resourceToAdd resource definition to add
   * @return the resource description object that was created
   */
  public ResourceDescription createReferencedBigQueryDataset(ResourceDescription resourceToAdd) {
    return createResource(
        resourceToAdd.getMetadata().getName(),
        () ->
            new WorkspaceManagerService(
                    globalContext.server, globalContext.requireCurrentTerraUser())
                .createReferencedBigQueryDataset(workspaceContext.getWorkspaceId(), resourceToAdd));
  }

  /**
   * Add a GCS bucket as a controlled resource in the workspace. Also updates the cached list of
   * resources.
   *
   * @param resourceToCreate resource definition to create
   * @param defaultStorageClass GCS storage class
   *     (https://cloud.google.com/storage/docs/storage-classes)
   * @param lifecycle list of lifecycle rules for the bucket
   *     (https://cloud.google.com/storage/docs/lifecycle)
   * @param location GCS bucket location (https://cloud.google.com/storage/docs/locations)
   * @return the resource description object that was created
   */
  public ResourceDescription createControlledGcsBucket(
      ResourceDescription resourceToCreate,
      @Nullable GcpGcsBucketDefaultStorageClass defaultStorageClass,
      GcsBucketLifecycle lifecycle,
      @Nullable String location) {
    return createResource(
        resourceToCreate.getMetadata().getName(),
        () ->
            new WorkspaceManagerService(
                    globalContext.server, globalContext.requireCurrentTerraUser())
                .createControlledGcsBucket(
                    workspaceContext.getWorkspaceId(),
                    resourceToCreate,
                    defaultStorageClass,
                    lifecycle,
                    location));
  }

  /**
   * Add a Big Query dataset as a controlled resource in the workspace. Also updates the cached list
   * of resources.
   *
   * @param resourceToCreate resource definition to create
   * @param location Big Query dataset location (https://cloud.google.com/bigquery/docs/locations)
   * @return the resource description object that was created
   */
  public ResourceDescription createControlledBigQueryDataset(
      ResourceDescription resourceToCreate, @Nullable String location) {
    return createResource(
        resourceToCreate.getMetadata().getName(),
        () ->
            new WorkspaceManagerService(
                    globalContext.server, globalContext.requireCurrentTerraUser())
                .createControlledBigQueryDataset(
                    workspaceContext.getWorkspaceId(), resourceToCreate, location));
  }

  public ResourceDescription createControlledAiNotebookInstance(
      ResourceDescription resourceToCreate,
      GcpAiNotebookInstanceCreationParameters creationParameters) {
    return createResource(
        resourceToCreate.getMetadata().getName(),
        () ->
            new WorkspaceManagerService(
                    globalContext.server, globalContext.requireCurrentTerraUser())
                .createControlledAiNotebookInstance(
                    workspaceContext.getWorkspaceId(), resourceToCreate, creationParameters));
  }

  /**
   * Create a new resource in the workspace. Also updates the cached list of resources.
   *
   * @param resourceName name of resource to create
   * @param createResource function pointer to execute the create request for a particular
   *     stewardship + resource type combination
   * @param <T> type of the create response object. this is usually very similar to the
   *     ResourceDescription object, but not quite identical.
   * @return the resource description object that was created
   */
  private <T> ResourceDescription createResource(String resourceName, Supplier<T> createResource) {
    workspaceContext.requireCurrentWorkspace();

    if (!isValidEnvironmentVariableName(resourceName)) {
      throw new UserActionableException(
          "Resource name can contain only alphanumeric and underscore characters.");
    }

    // create response object
    T createResponse = createResource.get();
    logger.info("Created resource: {}", createResponse);

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
        (resourceId) -> {
          new WorkspaceManagerService(globalContext.server, globalContext.requireCurrentTerraUser())
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
        (resourceId) -> {
          new WorkspaceManagerService(globalContext.server, globalContext.requireCurrentTerraUser())
              .deleteReferencedBigQueryDataset(workspaceContext.getWorkspaceId(), resourceId);
        });
  }

  /**
   * Delete a AI Notebook instance controlled resource in the workspace. Also updates the cached
   * list of resources.
   *
   * @param name name of the resource. this is unique across all resources in the workspace
   * @return the resource description object that was deleted
   */
  public ResourceDescription deleteControlledAiNotebookInstance(String name) {
    return deleteResource(
        name,
        (resourceId) -> {
          new WorkspaceManagerService(globalContext.server, globalContext.requireCurrentTerraUser())
              .deleteControlledAiNotebookInstance(workspaceContext.getWorkspaceId(), resourceId);
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
        (resourceId) -> {
          new WorkspaceManagerService(globalContext.server, globalContext.requireCurrentTerraUser())
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
        (resourceId) -> {
          new WorkspaceManagerService(globalContext.server, globalContext.requireCurrentTerraUser())
              .deleteControlledBigQueryDataset(workspaceContext.getWorkspaceId(), resourceId);
        });
  }

  /**
   * Delete a resource in the workspace. Also updates the cached list of resources.
   *
   * @param resourceName name of resource to delete
   * @param deleteResource function pointer to execute the delete request for a particular
   *     stewardship + resource type combination
   * @return the resource description object that was deleted
   */
  private ResourceDescription deleteResource(String resourceName, Consumer<UUID> deleteResource) {
    workspaceContext.requireCurrentWorkspace();

    // get the summary object
    ResourceDescription resourceToDelete = workspaceContext.getResource(resourceName);

    // delete the resource
    deleteResource.accept(resourceToDelete.getMetadata().getResourceId());

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
   * @param credentialsToUse enum value indicates whether to use end-user or pet SA credentials for
   *     checking access
   * @return true if the user can access the referenced GCS bucket with the given credentials
   */
  public boolean checkAccessToReferencedGcsBucket(
      ResourceDescription resource, CheckAccessCredentials credentialsToUse) {
    if (!resource.getMetadata().getStewardshipType().equals(StewardshipType.REFERENCED)) {
      throw new UserActionableException(
          "Unexpected stewardship type. Checking access is intended for REFERENCED resources only.");
    }

    // TODO (PF-717): replace this with a call to WSM once an endpoint is available
    TerraUser currentUser = globalContext.requireCurrentTerraUser();
    GoogleCredentials credentials =
        credentialsToUse.equals(CheckAccessCredentials.USER)
            ? currentUser.userCredentials
            : currentUser.petSACredentials;

    return new GoogleCloudStorage(credentials, workspaceContext.getGoogleProject())
        .checkObjectsListAccess(getGcsBucketUrl(resource));
  }

  /**
   * Check whether a user can access the referenced Big Query dataset, either with their end-user or
   * pet SA credentials.
   *
   * @param resource resource to check access for
   * @param credentialsToUse enum value indicates whether to use end-user or pet SA credentials for
   *     checking access
   * @return true if the user can access the referenced Big Query dataset with the given credentials
   */
  public boolean checkAccessToReferencedBigQueryDataset(
      ResourceDescription resource, CheckAccessCredentials credentialsToUse) {
    if (!resource.getMetadata().getStewardshipType().equals(StewardshipType.REFERENCED)) {
      throw new UserActionableException(
          "Unexpected stewardship type. Checking access is intended for REFERENCED resources only.");
    }

    // TODO (PF-717): replace this with a call to WSM once an endpoint is available
    TerraUser currentUser = globalContext.requireCurrentTerraUser();
    GoogleCredentials credentials =
        credentialsToUse.equals(CheckAccessCredentials.USER)
            ? currentUser.userCredentials
            : currentUser.petSACredentials;

    GcpBigQueryDatasetAttributes gcpBigQueryDatasetAttributes =
        resource.getResourceAttributes().getGcpBqDataset();
    return new GoogleBigQuery(credentials, workspaceContext.getGoogleProject())
        .checkListTablesAccess(
            DatasetId.of(
                gcpBigQueryDatasetAttributes.getProjectId(),
                gcpBigQueryDatasetAttributes.getDatasetId()));
  }

  /**
   * Helper enum for the {@link #checkAccessToReferencedGcsBucket} method. Specifies whether to use
   * end-user or pet SA credentials for checking access to a resource in the workspace.
   */
  public enum CheckAccessCredentials {
    USER,
    PET_SA;
  };

  /** Prefix for GCS bucket to make a valid URL. */
  private static final String GCS_BUCKET_URL_PREFIX = "gs://";

  /**
   * Delimiter between the project id and dataset id for a Big Query dataset.
   *
   * <p>The choice is somewhat arbitrary. BigQuery Datatsets do not have true URIs. The '.'
   * delimiter allows the path to be used directly in SQL calls with a Big Query extension.
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
   * Utility method for getting the SQL path to a Big Query dataset: [GCP project id].[BQ dataset
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

  /**
   * Utility method for getting the instance name
   * projects/[project_id]/locations/[location]/instances/[instanceId] for an AI notebook instance
   * resource.
   *
   * @param resource AI Notebook instance resource
   * @return full name of the instance
   */
  public static String getAiNotebookInstanceName(ResourceDescription resource) {
    GcpAiNotebookInstanceAttributes notebookAttributes =
        resource.getResourceAttributes().getGcpAiNotebookInstance();
    return String.format(
        "projects/%s/locations/%s/instances/%s",
        notebookAttributes.getProjectId(),
        notebookAttributes.getLocation(),
        notebookAttributes.getInstanceId());
  }
}
