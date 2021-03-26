package bio.terra.cli.service.utils;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.cli.context.TerraUser;
import bio.terra.workspace.api.UnauthenticatedApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.CloudPlatform;
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
import bio.terra.workspace.model.UpdateWorkspaceRequestBody;
import bio.terra.workspace.model.WorkspaceDescription;
import bio.terra.workspace.model.WorkspaceDescriptionList;
import bio.terra.workspace.model.WorkspaceStageModel;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import java.util.UUID;
import org.broadinstitute.dsde.workbench.client.sam.model.UserStatusDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Workspace Manager endpoints. */
public class WorkspaceManagerService {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerService.class);

  // the Terra environment where the WSM service lives
  private final ServerSpecification server;

  // the Terra user whose credentials will be used to call authenticated requests
  private final TerraUser terraUser;

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
    this.server = server;
    this.terraUser = terraUser;
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
   * @return the Workspace Manager version object
   */
  public SystemVersion getVersion() {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    try {
      return unauthenticatedApi.serviceVersion();
    } catch (ApiException ex) {
      throw new SystemException("Error getting Workspace Manager version", ex);
    }
  }

  /**
   * Call the Workspace Manager "/status" endpoint to get the status of the server.
   *
   * @return the Workspace Manager status object
   */
  public SystemStatus getStatus() {
    UnauthenticatedApi unauthenticatedApi = new UnauthenticatedApi(apiClient);
    try {
      return unauthenticatedApi.serviceStatus();
    } catch (ApiException ex) {
      throw new SystemException("Error getting Workspace Manager status", ex);
    }
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1" endpoint to list all the workspaces a user
   * can read.
   *
   * @param offset the offset to use when listing workspaces (zero to start from the beginning)
   * @param limit the maximum number of workspaces to return
   * @return the Workspace Manager workspsace list object
   */
  public WorkspaceDescriptionList listWorkspaces(int offset, int limit) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      return workspaceApi.listWorkspaces(offset, limit);
    } catch (ApiException ex) {
      throw new SystemException("Error fetching list of workspaces", ex);
    }
  }

  /**
   * Call the Workspace Manager POST "/api/workspaces/v1" endpoint to create a new workspace, then
   * call the POST "/api/workspaces/v1/{workspaceId}/cloudcontexts" endpoint to create a new backing
   * Google context. Poll the "/api/workspaces/v1/{workspaceId}/cloudcontexts/results/{jobId}"
   * endpoint to wait for the job to finish.
   *
   * @param displayName optional display name
   * @param description optional description
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription createWorkspace(String displayName, String description) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      // create the Terra workspace object
      UUID workspaceId = UUID.randomUUID();
      CreateWorkspaceRequestBody workspaceRequestBody = new CreateWorkspaceRequestBody();
      workspaceRequestBody.setId(workspaceId);
      workspaceRequestBody.setStage(WorkspaceStageModel.MC_WORKSPACE);
      workspaceRequestBody.setSpendProfile("wm-default-spend-profile");
      workspaceRequestBody.setDisplayName(displayName);
      workspaceRequestBody.setDescription(description);
      workspaceApi.createWorkspace(workspaceRequestBody);

      // create the Google project that backs the Terra workspace object
      UUID jobId = UUID.randomUUID();
      CreateCloudContextRequest cloudContextRequest = new CreateCloudContextRequest();
      cloudContextRequest.setCloudPlatform(CloudPlatform.GCP);
      cloudContextRequest.setJobControl(new JobControl().id(jobId.toString()));
      workspaceApi.createCloudContext(cloudContextRequest, workspaceId);

      // poll the job result endpoint until the job status is completed
      final int MAX_JOB_POLLING_TRIES = 120; // maximum 120 seconds sleep
      int numJobPollingTries = 1;
      CreateCloudContextResult cloudContextResult;
      JobReport.StatusEnum jobReportStatus;
      do {
        logger.info(
            "Job polling try #{}, workspace id: {}, job id: {}",
            numJobPollingTries,
            workspaceId,
            jobId);
        cloudContextResult =
            workspaceApi.getCreateCloudContextResult(workspaceId, jobId.toString());
        jobReportStatus = cloudContextResult.getJobReport().getStatus();
        logger.debug("Create workspace cloudContextResult: {}", cloudContextResult);
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
      return workspaceApi.getWorkspace(workspaceId);
    } catch (ApiException | InterruptedException ex) {
      throw new SystemException("Error creating a new workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1/{id}" endpoint to fetch an existing
   * workspace.
   *
   * @param workspaceId the id of the workspace to fetch
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription getWorkspace(UUID workspaceId) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      // fetch the Terra workspace object
      WorkspaceDescription workspaceWithContext = workspaceApi.getWorkspace(workspaceId);
      String googleProjectId =
          (workspaceWithContext.getGcpContext() == null)
              ? null
              : workspaceWithContext.getGcpContext().getProjectId();
      logger.info(
          "Workspace context: {}, project id: {}", workspaceWithContext.getId(), googleProjectId);
      return workspaceWithContext;
    } catch (ApiException ex) {
      throw new SystemException("Error fetching workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager DELETE "/api/workspaces/v1/{id}" endpoint to delete an existing
   * workspace.
   *
   * @param workspaceId the id of the workspace to delete
   */
  public void deleteWorkspace(UUID workspaceId) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      // delete the Terra workspace object
      workspaceApi.deleteWorkspace(workspaceId);
    } catch (ApiException ex) {
      throw new SystemException("Error deleting workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager PATCH "/api/workspaces/v1/{id}" endpoint to update an existing
   * workspace.
   *
   * @param workspaceId the id of the workspace to update
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription updateWorkspace(
      UUID workspaceId, String displayName, String description) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      // update the Terra workspace object
      UpdateWorkspaceRequestBody updateRequest =
          new UpdateWorkspaceRequestBody().displayName(displayName).description(description);
      return workspaceApi.updateWorkspace(updateRequest, workspaceId);
    } catch (ApiException ex) {
      throw new SystemException("Error updating workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager POST "/api/workspaces/v1/{id}/roles/{role}/members" endpoint to
   * grant an IAM role.
   *
   * @param workspaceId the workspace to update
   * @param userEmail the user email to add
   * @param iamRole the role to assign
   */
  public void grantIamRole(UUID workspaceId, String userEmail, IamRole iamRole) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    GrantRoleRequestBody grantRoleRequestBody = new GrantRoleRequestBody().memberEmail(userEmail);
    try {
      workspaceApi.grantRole(grantRoleRequestBody, workspaceId, iamRole);
    } catch (ApiException ex) {
      // a bad request is the only type of exception that inviting the user might fix
      if (!isBadRequest(ex)) {
        throw new SystemException("Error granting IAM role on workspace", ex);
      }

      try {
        // try to invite the user first, in case they are not already registered
        // if they are already registered, this will throw an exception
        logger.info("Inviting new user: {}", userEmail);
        UserStatusDetails userStatusDetails =
            new SamService(server, terraUser).inviteUser(userEmail);
        logger.info("Invited new user: {}", userStatusDetails);

        // now try to add the user to the workspace role
        // retry if it returns with a bad request (because the invite sometimes takes a few seconds
        // to propagate -- not sure why)
        HttpUtils.callWithRetries(
            () -> {
              workspaceApi.grantRole(grantRoleRequestBody, workspaceId, iamRole);
              return null;
            },
            WorkspaceManagerService::isBadRequest);
      } catch (ApiException | InterruptedException inviteEx) {
        throw new SystemException("Error granting IAM role on workspace", inviteEx);
      }
    }
  }

  /**
   * Utility method that checks if an exception thrown by the WSM client is a bad request.
   *
   * @param ex exception to test
   * @return true if the exception is a bad request
   */
  private static boolean isBadRequest(Exception ex) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    int statusCode = ((ApiException) ex).getCode();
    return statusCode == HttpStatusCodes.STATUS_CODE_BAD_REQUEST;
  }

  /**
   * Call the Workspace Manager DELETE "/api/workspaces/v1/{id}/roles/{role}/members/{memberEmail}"
   * endpoint to remove an IAM role.
   *
   * @param workspaceId the workspace to update
   * @param userEmail the user email to remove
   * @param iamRole the role to remove
   */
  public void removeIamRole(UUID workspaceId, String userEmail, IamRole iamRole) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      workspaceApi.removeRole(workspaceId, iamRole, userEmail);
    } catch (ApiException ex) {
      throw new SystemException("Error removing IAM role on workspace", ex);
    }
  }

  /**
   * Call the Workspace Manager "/api/workspace/v1/{id}/roles" endpoint to get a list of roles and
   * their members.
   *
   * @param workspaceId the workspace to query
   * @return a list of roles and the users that have them
   */
  public RoleBindingList getRoles(UUID workspaceId) {
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    try {
      return workspaceApi.getRoles(workspaceId);
    } catch (ApiException ex) {
      throw new SystemException("Error fetching users and their IAM roles for workspace", ex);
    }
  }
}
