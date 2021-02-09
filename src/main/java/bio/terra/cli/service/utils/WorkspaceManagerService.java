package bio.terra.cli.service.utils;

import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.context.TerraUser;
import bio.terra.workspace.api.UnauthenticatedApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.model.CloudContext;
import bio.terra.workspace.model.CreateCloudContextRequest;
import bio.terra.workspace.model.CreateCloudContextResult;
import bio.terra.workspace.model.CreateWorkspaceRequestBody;
import bio.terra.workspace.model.GrantRoleRequestBody;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.JobControl;
import bio.terra.workspace.model.JobReport;
import bio.terra.workspace.model.RoleBindingList;
import bio.terra.workspace.model.SystemStatus;
import bio.terra.workspace.model.SystemVersion;
import bio.terra.workspace.model.WorkspaceDescription;
import bio.terra.workspace.model.WorkspaceStageModel;
import com.google.auth.oauth2.AccessToken;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Workspace Manager endpoints. */
public class WorkspaceManagerService {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerService.class);

  // the client object used for talking to WSM
  private final ApiClient apiClient;

  /**
   * Constructor for class that talks to the Workspace Manager service. The user must be
   * authenticated. Methods in this class will use its credentials to call authenticated endpoints.
   *
   * @param server the Terra environment where the Workspace Manager service lives
   * @param terraUser the Terra user whose credentials will be used to call authenticated endpoints
   */
  public WorkspaceManagerService(ServerSpecification server, TerraUser terraUser) {
    this.apiClient = new ApiClient();
    buildClientForTerraUser(server, terraUser);
  }

  /**
   * Constructor for class that talks to the Workspace Manager service. No user is specified, so
   * only unauthenticated endpoints can be called.
   *
   * @param server the Terra environment where the Workspace Manager service lives
   */
  public WorkspaceManagerService(ServerSpecification server) {
    this(server, null);
  }
  /**
   * Build the Workspace Manager API client object for the given Terra user and global context. If
   * terraUser is null, this method returns the client object without an access token set.
   *
   * @param server the Terra environment where the Workspace Manager service lives
   * @param terraUser the Terra user whose credentials will be used to call authenticated endpoints
   */
  private void buildClientForTerraUser(ServerSpecification server, TerraUser terraUser) {
    this.apiClient.setBasePath(server.workspaceManagerUri);

    if (terraUser != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      AccessToken userAccessToken = terraUser.fetchUserAccessToken();
      this.apiClient.setAccessToken(userAccessToken.getTokenValue());
    }
  }

  /**
   * Call the Workspace Manager "/version" endpoint to get the version of the server that is
   * currently running.
   *
   * @param apiClient the WSM client with credentials set
   * @return the Workspace Manager version object
   */
  public SystemVersion getVersion() {
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
   * @param apiClient the WSM client with credentials set
   * @return the Workspace Manager status object
   */
  public SystemStatus getStatus() {
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
   * @param apiClient the WSM client with credentials set
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription createWorkspace() {
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
      CreateCloudContextRequest cloudContextRequest = new CreateCloudContextRequest();
      cloudContextRequest.setCloudContext(CloudContext.GOOGLE);
      cloudContextRequest.setJobControl(new JobControl().id(jobId.toString()));
      workspaceApi.createCloudContext(cloudContextRequest, workspaceId);

      // poll the job result endpoint until the job status is completed
      final int MAX_JOB_POLLING_TRIES = 120; // maximum 120 seconds sleep
      int numJobPollingTries = 1;
      CreateCloudContextResult cloudContextResult;
      JobReport.StatusEnum jobReportStatus;
      do {
        logger.info(
            "job polling try #{}, workspace id: {}, job id: {}",
            numJobPollingTries,
            workspaceId,
            jobId);
        cloudContextResult = workspaceApi.createCloudContextResult(workspaceId, jobId.toString());
        jobReportStatus = cloudContextResult.getJobReport().getStatus();
        logger.debug("create workspace cloudContextResult: {}", cloudContextResult);
        numJobPollingTries++;
        if (jobReportStatus.equals(JobReport.StatusEnum.RUNNING)) {
          Thread.sleep(1000);
        }
      } while (jobReportStatus.equals(JobReport.StatusEnum.RUNNING)
          && numJobPollingTries < MAX_JOB_POLLING_TRIES);

      if (jobReportStatus.equals(JobReport.StatusEnum.FAILED)) {
        logger.error(
            "Job to create a new workspace failed: {}", cloudContextResult.getErrorReport());
      } else if (jobReportStatus.equals(JobReport.StatusEnum.RUNNING)) {
        logger.error("Job to create a new workspace timed out in the CLI");
      }

      // call the get workspace endpoint to get the full description object
      workspaceWithContext = workspaceApi.getWorkspace(workspaceId);
    } catch (Exception ex) {
      logger.error("Error creating a new workspace", ex);
    }
    return workspaceWithContext;
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1/{id}" endpoint to fetch an existing
   * workspace.
   *
   * @param apiClient the WSM client with credentials set
   * @param workspaceId the id of the workspace to fetch
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription getWorkspace(UUID workspaceId) {
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
   *
   * @param apiClient the WSM client with credentials set
   * @param workspaceId the id of the workspace to delete
   */
  public void deleteWorkspace(UUID workspaceId) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      // delete the Terra workspace object
      workspaceApi.deleteWorkspace(workspaceId);
    } catch (Exception ex) {
      logger.error("Error deleting workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager POST "/api/workspaces/v1/{id}/roles/{role}/members" endpoint to
   * grant an IAM role.
   *
   * @param apiClient the WSM client with credentials set
   * @param workspaceId the workspace to update
   * @param userEmail the user email to add
   * @param iamRole the role to assign
   */
  public void grantIamRole(UUID workspaceId, String userEmail, IamRole iamRole) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      GrantRoleRequestBody grantRoleRequestBody = new GrantRoleRequestBody().memberEmail(userEmail);
      workspaceApi.grantRole(grantRoleRequestBody, workspaceId, iamRole);
    } catch (Exception ex) {
      logger.error("Error granting IAM role on workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager DELETE "/api/workspaces/v1/{id}/roles/{role}/members/{memberEmail}"
   * endpoint to remove an IAM role.
   *
   * @param apiClient the WSM client with credentials set
   * @param workspaceId the workspace to update
   * @param userEmail the user email to remove
   * @param iamRole the role to remove
   */
  public static void removeIamRole(
      ApiClient apiClient, UUID workspaceId, String userEmail, IamRole iamRole) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      workspaceApi.removeRole(workspaceId, iamRole, userEmail);
    } catch (Exception ex) {
      logger.error("Error removing IAM role on workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager "/api/workspace/v1/{id}/roles" endpoint to get a list of roles and
   * their members.
   *
   * @param apiClient the WSM client with credentials set
   * @param workspaceId the workspace to query
   * @return a list of roles and the users that have them
   */
  public static RoleBindingList getRoles(ApiClient apiClient, UUID workspaceId) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    RoleBindingList roleBindings = null;
    try {
      roleBindings = workspaceApi.getRoles(workspaceId);
    } catch (Exception ex) {
      logger.error("Error granting IAM role on workspace", ex);
    }
    return roleBindings;
  }
}
