package bio.terra.cli.service;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.WorkspaceManagerService;
import bio.terra.workspace.model.GcpAiNotebookInstanceAttributes;
import bio.terra.workspace.model.GcpAiNotebookInstanceCreationParameters;
import bio.terra.workspace.model.ResourceDescription;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
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
