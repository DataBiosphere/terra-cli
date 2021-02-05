package bio.terra.cli.utils;

import bio.terra.cli.model.ServerSpecification;
import bio.terra.cli.model.TerraUser;
import bio.terra.workspace.api.UnauthenticatedApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.model.CreateGoogleContextRequestBody;
import bio.terra.workspace.model.CreateWorkspaceRequestBody;
import bio.terra.workspace.model.GrantRoleRequestBody;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.SystemStatus;
import bio.terra.workspace.model.SystemVersion;
import bio.terra.workspace.model.WorkspaceDescription;
import bio.terra.workspace.model.WorkspaceStageModel;
import com.google.auth.oauth2.AccessToken;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Workspace Manager endpoints. */
public class WorkspaceManagerUtils {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerUtils.class);

  private WorkspaceManagerUtils() {}

  /**
   * Build the Workspace Manager API client object for the given Terra user and global context. If
   * terraUser is null, this method returns the client object without an access token set.
   *
   * @param terraUser the Terra user whose credentials are supplied to the API client object
   * @param server the server specification that holds a pointer to the Workspace Manger instance
   * @return the API client object for this user
   */
  public static ApiClient getClientForTerraUser(TerraUser terraUser, ServerSpecification server) {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(server.workspaceManagerUri);

    if (terraUser != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      AccessToken userAccessToken = terraUser.fetchUserAccessToken();
      apiClient.setAccessToken(userAccessToken.getTokenValue());
    }
    return apiClient;
  }

  /**
   * Call the Workspace Manager "/version" endpoint to get the version of the server that is
   * currently running.
   *
   * @return the Workspace Manager version object
   */
  public static SystemVersion getVersion(ApiClient apiClient) {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    SystemVersion systemVersion = null;
    try {
      systemVersion = unauthenticatedApi.serviceVersion();
    } catch (Exception ex) {
      logger.error("Error getting Workspace Manager version", ex);
    }
    return systemVersion;
  }

  /**
   * Call the Workspace Manager "/status" endpoint to get the status of the server.
   *
   * @return the Workspace Manager status object
   */
  public static SystemStatus getStatus(ApiClient apiClient) {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    SystemStatus status = null;
    try {
      status = unauthenticatedApi.serviceStatus();
    } catch (Exception ex) {
      logger.error("Error getting Workspace Manager status", ex);
    }
    return status;
  }

  /**
   * Call the Workspace Manager "/api/workspaces/v1" endpoint to create a new workspace, then poll
   * the "/api/workspaces/v1/{id}" endpoint until the Google context project id is populated.
   *
   * @return the Workspace Manager workspace description object
   */
  public static WorkspaceDescription createWorkspace(ApiClient apiClient) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    WorkspaceDescription workspaceWithContext = null;
    try {
      // create the Terra workspace object
      UUID workspaceId = UUID.randomUUID();
      CreateWorkspaceRequestBody workspaceRequestBody = new CreateWorkspaceRequestBody();
      workspaceRequestBody.setId(workspaceId);
      workspaceRequestBody.setStage(WorkspaceStageModel.MC_WORKSPACE);
      workspaceRequestBody.setSpendProfile("wm-default-spend-profile");
      workspaceApi.createWorkspace(workspaceRequestBody);

      // create the Google project that backs the Terra workspace object
      UUID jobId = UUID.randomUUID();
      CreateGoogleContextRequestBody contextRequestBody = new CreateGoogleContextRequestBody();
      contextRequestBody.setJobId(jobId.toString());
      workspaceApi.createGoogleContext(contextRequestBody, workspaceId);

      // poll the get workspace endpoint until the project id property is populated
      final int MAX_JOB_POLLING_TRIES = 120; // maximum 120 seconds sleep
      int numJobPollingTries = 1;
      String googleProjectId;
      do {
        workspaceWithContext = workspaceApi.getWorkspace(workspaceId);
        googleProjectId =
            (workspaceWithContext.getGoogleContext() == null)
                ? null
                : workspaceWithContext.getGoogleContext().getProjectId();
        logger.info(
            "job polling try #{}, workspace context: {}, project id: {}",
            numJobPollingTries,
            workspaceWithContext.getId(),
            googleProjectId);
        numJobPollingTries++;
        if (googleProjectId == null) {
          Thread.sleep(1000);
        }
      } while (googleProjectId == null && numJobPollingTries < MAX_JOB_POLLING_TRIES);
    } catch (Exception ex) {
      logger.error("Error creating a new workspace", ex);
    }
    return workspaceWithContext;
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1/{id}" endpoint to fetch an existing
   * workspace.
   *
   * @return the Workspace Manager workspace description object
   */
  public static WorkspaceDescription fetchWorkspace(ApiClient apiClient, UUID workspaceId) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    WorkspaceDescription workspaceWithContext = null;
    try {
      // fetch the Terra workspace object
      workspaceWithContext = workspaceApi.getWorkspace(workspaceId);
      String googleProjectId =
          (workspaceWithContext.getGoogleContext() == null)
              ? null
              : workspaceWithContext.getGoogleContext().getProjectId();
      logger.info(
          "workspace context: {}, project id: {}", workspaceWithContext.getId(), googleProjectId);
    } catch (Exception ex) {
      logger.error("Error fetching workspace", ex);
    }
    return workspaceWithContext;
  }

  /**
   * Call the Workspace Manager DELETE "/api/workspaces/v1/{id}" endpoint to delete an existing
   * workspace.
   */
  public static void deleteWorkspace(ApiClient apiClient, UUID workspaceId) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    WorkspaceDescription workspaceWithContext = null;
    try {
      // delete the Terra workspace object
      workspaceApi.deleteWorkspace(workspaceId);
    } catch (Exception ex) {
      logger.error("Error deleting workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager "/api/workspaces/v1/{id}/roles/{role}/members" endpoint to grant an
   * IAM role.
   */
  public static void grantIamRole(
      ApiClient apiClient, UUID workspaceId, String userEmail, IamRole iamRole) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      GrantRoleRequestBody grantRoleRequestBody = new GrantRoleRequestBody().memberEmail(userEmail);
      workspaceApi.grantRole(grantRoleRequestBody, workspaceId, iamRole);
    } catch (Exception ex) {
      logger.error("Error granting IAM role on workspace", ex);
    }
  }
}
