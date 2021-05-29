package bio.terra.cli.service;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Resource;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.GoogleBigQuery;
import bio.terra.cli.service.utils.WorkspaceManagerService;
import bio.terra.workspace.model.GcpAiNotebookInstanceAttributes;
import bio.terra.workspace.model.GcpAiNotebookInstanceCreationParameters;
import bio.terra.workspace.model.GcpBigQueryDatasetAttributes;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.StewardshipType;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class manipulates the workspace properties of the global context object. */
public class WorkspaceManager {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManager.class);

  private final GlobalContext globalContext;
  private final WorkspaceContext workspaceContext;

  public WorkspaceManager(GlobalContext globalContext, WorkspaceContext workspaceContext) {
    this.globalContext = globalContext;
    this.workspaceContext = workspaceContext;
  }

  /**
   * Update the cached list of resources (controlled & referenced) in the workspace, by fetching the
   * up-to-date list from WSM.
   */
  private void updateResourcesCache() {
    // check that there is a workspace currently mounted
    workspaceContext.requireCurrentWorkspace();

    // call WSM to get the list of resources for the existing workspace
    List<ResourceDescription> resourceList =
        new WorkspaceManagerService()
            .enumerateAllResources(
                workspaceContext.getWorkspaceId(), globalContext.resourcesCacheSize);

    // update the cache with the list of resources fetched from WSM
    workspaceContext.updateResources(resourceList);
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
            new WorkspaceManagerService()
                .createReferencedBigQueryDataset(workspaceContext.getWorkspaceId(), resourceToAdd));
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
            new WorkspaceManagerService()
                .createControlledBigQueryDataset(
                    workspaceContext.getWorkspaceId(), resourceToCreate, location));
  }

  public ResourceDescription createControlledAiNotebookInstance(
      ResourceDescription resourceToCreate,
      GcpAiNotebookInstanceCreationParameters creationParameters) {
    return createResource(
        resourceToCreate.getMetadata().getName(),
        () ->
            new WorkspaceManagerService()
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
          new WorkspaceManagerService()
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
          new WorkspaceManagerService()
              .deleteControlledAiNotebookInstance(workspaceContext.getWorkspaceId(), resourceId);
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
          new WorkspaceManagerService()
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
   * Check whether a user can access the referenced Big Query dataset, either with their end-user or
   * pet SA credentials.
   *
   * @param resource resource to check access for
   * @param credentialsToUse enum value indicates whether to use end-user or pet SA credentials for
   *     checking access
   * @return true if the user can access the referenced Big Query dataset with the given credentials
   */
  public boolean checkAccessToReferencedBigQueryDataset(
      ResourceDescription resource, Resource.CheckAccessCredentials credentialsToUse) {
    if (!resource.getMetadata().getStewardshipType().equals(StewardshipType.REFERENCED)) {
      throw new UserActionableException(
          "Unexpected stewardship type. Checking access is intended for REFERENCED resources only.");
    }

    // TODO (PF-717): replace this with a call to WSM once an endpoint is available
    TerraUser currentUser = globalContext.requireCurrentTerraUser();
    GoogleCredentials credentials =
        credentialsToUse.equals(Resource.CheckAccessCredentials.USER)
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
   * Delimiter between the project id and dataset id for a Big Query dataset.
   *
   * <p>The choice is somewhat arbitrary. BigQuery Datatsets do not have true URIs. The '.'
   * delimiter allows the path to be used directly in SQL calls with a Big Query extension.
   */
  private static final char BQ_PROJECT_DATASET_DELIMITER = '.';

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
