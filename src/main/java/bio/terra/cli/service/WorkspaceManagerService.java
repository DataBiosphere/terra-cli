package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.AddGitRepoParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGitRepoParams;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.utils.HttpClients;
import bio.terra.cli.utils.JacksonMapper;
import bio.terra.workspace.api.FolderApi;
import bio.terra.workspace.api.ReferencedGcpResourceApi;
import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.api.UnauthenticatedApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.CloneWorkspaceRequest;
import bio.terra.workspace.model.CloneWorkspaceResult;
import bio.terra.workspace.model.CloudPlatform;
import bio.terra.workspace.model.ControlledResourceCommonFields;
import bio.terra.workspace.model.CreateCloudContextRequest;
import bio.terra.workspace.model.CreateCloudContextResult;
import bio.terra.workspace.model.CreateGitRepoReferenceRequestBody;
import bio.terra.workspace.model.CreateWorkspaceRequestBody;
import bio.terra.workspace.model.ErrorReport;
import bio.terra.workspace.model.Folder;
import bio.terra.workspace.model.FolderList;
import bio.terra.workspace.model.GitRepoAttributes;
import bio.terra.workspace.model.GitRepoResource;
import bio.terra.workspace.model.GrantRoleRequestBody;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.JobControl;
import bio.terra.workspace.model.JobReport;
import bio.terra.workspace.model.JobReport.StatusEnum;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.Property;
import bio.terra.workspace.model.ReferenceResourceCommonFields;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceList;
import bio.terra.workspace.model.RoleBindingList;
import bio.terra.workspace.model.SystemVersion;
import bio.terra.workspace.model.UpdateGitRepoReferenceRequestBody;
import bio.terra.workspace.model.UpdateWorkspaceRequestBody;
import bio.terra.workspace.model.WorkspaceDescription;
import bio.terra.workspace.model.WorkspaceDescriptionList;
import bio.terra.workspace.model.WorkspaceStageModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import com.google.common.collect.ImmutableList;
import java.net.SocketException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Workspace Manager endpoints. */
public class WorkspaceManagerService {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerService.class);
  // maximum number of resources to fetch per call to the enumerate endpoint
  private static final int MAX_RESOURCES_PER_ENUMERATE_REQUEST = 100;
  // the Terra environment where the WSM service lives
  private final Server server;
  // the client object used for talking to WSM
  protected final ApiClient apiClient;

  /**
   * Constructor for class that talks to WSM. If the access token is null, only unauthenticated
   * endpoints can be called.
   */
  protected WorkspaceManagerService(@Nullable AccessToken accessToken, Server server) {
    this.server = server;
    this.apiClient = new ApiClient();

    this.apiClient.setHttpClient(HttpClients.getWsmClient());
    this.apiClient.setBasePath(server.getWorkspaceManagerUri());
    if (accessToken != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      this.apiClient.setAccessToken(accessToken.getTokenValue());
    }
  }

  /**
   * Factory method for class that talks to WSM. No user credentials are used, so only
   * unauthenticated endpoints can be called.
   */
  public static WorkspaceManagerService unauthenticated(Server server) {
    return new WorkspaceManagerService(null, server);
  }

  /**
   * Factory method for class that talks to WSM. Pulls the current server and user from the context.
   */
  public static WorkspaceManagerService fromContext() {
    return new WorkspaceManagerService(Context.requireUser().getTerraToken(), Context.getServer());
  }

  /**
   * Call the Workspace Manager "/version" endpoint to get the version of the server that is
   * currently running.
   *
   * @return the Workspace Manager version object
   */
  public SystemVersion getVersion() {
    return callWithRetries(
        new UnauthenticatedApi(apiClient)::serviceVersion,
        "Error getting Workspace Manager version");
  }

  /** Call the Workspace Manager "/status" endpoint to get the status of the server. */
  public void getStatus() {
    callWithRetries(
        new UnauthenticatedApi(apiClient)::serviceStatus, "Error getting Workspace Manager status");
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1" endpoint to list all the workspaces a user
   * can read.
   *
   * @param offset the offset to use when listing workspaces (zero to start from the beginning)
   * @param limit the maximum number of workspaces to return
   * @return the Workspace Manager workspace list object
   */
  public WorkspaceDescriptionList listWorkspaces(int offset, int limit) {
    return callWithRetries(
        () ->
            new WorkspaceApi(apiClient).listWorkspaces(offset, limit, /*minimumHighestRole=*/ null),
        "Error fetching list of workspaces");
  }

  /**
   * Call the Workspace Manager POST "/api/workspaces/v1" endpoint to create a new workspace, then
   * call the POST "/api/workspaces/v1/{workspaceId}/cloudcontexts" endpoint to create a new backing
   * Google context. Poll the "/api/workspaces/v1/{workspaceId}/cloudcontexts/results/{jobId}"
   * endpoint to wait for the job to finish.
   *
   * @param userFacingId required user-facing ID
   * @param name optional display name
   * @param description optional description
   * @param properties optional properties
   * @param spendProfile spend profile
   * @return the Workspace Manager workspace description object
   * @throws SystemException if the job to create the workspace cloud context fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public WorkspaceDescription createWorkspace(
      String userFacingId,
      CloudPlatform cloudPlatform,
      @Nullable String name,
      @Nullable String description,
      @Nullable Map<String, String> properties,
      String spendProfile) {
    return handleClientExceptions(
        () -> {
          // create the Terra workspace object
          UUID workspaceId = UUID.randomUUID();
          CreateWorkspaceRequestBody workspaceRequestBody = new CreateWorkspaceRequestBody();
          workspaceRequestBody.setId(workspaceId);
          workspaceRequestBody.setUserFacingId(userFacingId);
          workspaceRequestBody.setStage(WorkspaceStageModel.MC_WORKSPACE);
          workspaceRequestBody.setSpendProfile(spendProfile);
          workspaceRequestBody.setDisplayName(name);
          workspaceRequestBody.setDescription(description);
          workspaceRequestBody.setProperties(Workspace.stringMapToProperties(properties));

          // make the create workspace request
          WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
          try {
            HttpUtils.callWithRetries(
                () -> workspaceApi.createWorkspace(workspaceRequestBody),
                WorkspaceManagerService::isRetryable);
          } catch (ApiException e) {
            // TODO(PF-2460): This check is currently on the createWorkspace call, but will likely
            //   be moved to the createContext call in the future.

            // Surface a more user-friendly error if the user does not have access to the
            // appropriate spend profile.
            if (e.getCode() == HttpStatusCodes.STATUS_CODE_FORBIDDEN
                && e.getMessage().contains("spend profile")) {
              throw new UserActionableException(
                  "Accessing the spend profile failed. Ask an administrator to grant you access.");
            } else {
              throw e;
            }
          }

          // create the cloud context that backs the Terra workspace object
          UUID jobId = UUID.randomUUID();
          CreateCloudContextRequest cloudContextRequest = new CreateCloudContextRequest();
          cloudContextRequest.setCloudPlatform(cloudPlatform);
          cloudContextRequest.setJobControl(new JobControl().id(jobId.toString()));

          // make the initial create context request
          HttpUtils.callWithRetries(
              () -> workspaceApi.createCloudContext(cloudContextRequest, workspaceId),
              WorkspaceManagerService::isRetryable);

          // poll the result endpoint until the job is no longer RUNNING
          CreateCloudContextResult createContextResult =
              HttpUtils.pollWithRetries(
                  () -> workspaceApi.getCreateCloudContextResult(workspaceId, jobId.toString()),
                  (result) -> isDone(result.getJobReport()),
                  WorkspaceManagerService::isRetryable,
                  // Context creation will wait for cloud IAM permissions to sync, so poll for up to
                  // 30 minutes.
                  /*maxCalls=*/ 30,
                  /*sleepDuration=*/ Duration.ofSeconds(60));
          logger.debug("create workspace context result: {}", createContextResult);
          StatusEnum status = createContextResult.getJobReport().getStatus();
          if (StatusEnum.FAILED == status) {
            // need to delete the empty workspace before continuing
            boolean workspaceSuccessfullyDeleted = false;
            try {
              deleteWorkspace(workspaceId);
              workspaceSuccessfullyDeleted = true;
            } catch (RuntimeException ex) {
              logger.error(
                  "Failed to delete workspace {} when cleaning up failed creation of cloud context. ",
                  workspaceId,
                  ex);
            }
          }
          throwIfJobNotCompleted(
              createContextResult.getJobReport(), createContextResult.getErrorReport());

          // call the get workspace endpoint to get the full description object
          return HttpUtils.callWithRetries(
              () -> workspaceApi.getWorkspace(workspaceId, /*minimumHighestRole=*/ null),
              WorkspaceManagerService::isRetryable);
        },
        "Error creating a new workspace");
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1/{id}" endpoint to fetch an existing
   * workspace.
   */
  public WorkspaceDescription getWorkspace(UUID uuid) {
    WorkspaceDescription workspaceWithContext =
        callWithRetries(
            () -> new WorkspaceApi(apiClient).getWorkspace(uuid, /*minimumHighestRole=*/ null),
            "Error fetching workspace");

    logWorkspaceDescription(workspaceWithContext);
    return workspaceWithContext;
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1/workspaceByUserFacingId/{userFacingId}"
   * endpoint to fetch an existing workspace.
   */
  public WorkspaceDescription getWorkspaceByUserFacingId(String userFacingId) {
    WorkspaceDescription workspaceWithContext =
        callWithRetries(
            () ->
                new WorkspaceApi(apiClient)
                    .getWorkspaceByUserFacingId(userFacingId, /*minimumHighestRole=*/ null),
            "Error fetching workspace");

    logWorkspaceDescription(workspaceWithContext);
    return workspaceWithContext;
  }

  private void logWorkspaceDescription(WorkspaceDescription workspaceWithContext) {
    String gcpContext =
        (workspaceWithContext.getGcpContext() == null)
            ? null
            : workspaceWithContext.getGcpContext().toString();
    String awsContext =
        (workspaceWithContext.getAwsContext() == null)
            ? null
            : workspaceWithContext.getAwsContext().toString();
    logger.info(
        "Workspace context: userFacingId {}, gcpContext: {}, awsContext: {}",
        workspaceWithContext.getUserFacingId(),
        gcpContext,
        awsContext);
  }

  /**
   * Call the Workspace Manager DELETE "/api/workspaces/v1/{id}" endpoint to delete an existing
   * workspace.
   *
   * @param workspaceId the id of the workspace to delete
   */
  public void deleteWorkspace(UUID workspaceId) {
    callWithRetries(
        () -> new WorkspaceApi(apiClient).deleteWorkspace(workspaceId), "Error deleting workspace");
  }

  /**
   * Call the Workspace Manager PATCH "/api/workspaces/v1/{id}" endpoint to update an existing
   * workspace.
   *
   * @param workspaceId the id of the workspace to update
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription updateWorkspace(
      UUID workspaceId,
      @Nullable String userFacingId,
      @Nullable String name,
      @Nullable String description) {
    UpdateWorkspaceRequestBody updateRequest =
        new UpdateWorkspaceRequestBody()
            .userFacingId(userFacingId)
            .displayName(name)
            .description(description);
    return callWithRetries(
        () -> new WorkspaceApi(apiClient).updateWorkspace(updateRequest, workspaceId),
        "Error updating workspace");
  }

  /**
   * Call the Workspace Manager POST "/api/workspaces/v1/{id}/properties" endpoint to update
   * properties in workspace.
   *
   * @param workspaceId the id of the workspace to update
   * @param workspaceProperties the update properties
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription updateWorkspaceProperties(
      UUID workspaceId, Map<String, String> workspaceProperties) {
    callWithRetries(
        () ->
            new WorkspaceApi(apiClient)
                .updateWorkspaceProperties(buildProperties(workspaceProperties), workspaceId),
        "Error updating workspace properties");
    return callWithRetries(
        () -> new WorkspaceApi(apiClient).getWorkspace(workspaceId, /*minimumHighestRole=*/ null),
        "Error getting the workspace after updating properties");
  }

  /**
   * Call the Workspace Manager POST "/api/workspaces/v1/{id}/gcp/enablepet" endpoint to grant the
   * currently logged in user and their pet permission to impersonate their own pet service account
   * in a workspace.
   *
   * @param workspaceId the id of the workspace to enable pet impersonation in
   */
  public void enablePet(UUID workspaceId) {
    callWithRetries(
        () -> new WorkspaceApi(apiClient).enablePet(workspaceId), "Error enabling user's pet SA");
  }

  /**
   * Call the Workspace Manager POST "/api/workspaces/v1/{workspaceId}/clone" endpoint to clone a
   * workspace.
   *
   * @param workspaceId - workspace ID to clone
   * @param userFacingId - required userFacingId of new cloned workspace
   * @param name - optional name of new cloned workspace
   * @param description - optional description for new workspace
   * @param spendProfile - spend profile to use for the workspace
   * @return object with information about the clone job success and destination workspace
   */
  public CloneWorkspaceResult cloneWorkspace(
      UUID workspaceId,
      String userFacingId,
      @Nullable String name,
      @Nullable String description,
      String spendProfile) {
    CloneWorkspaceRequest cloneRequest =
        new CloneWorkspaceRequest()
            .spendProfile(spendProfile)
            .userFacingId(userFacingId)
            .displayName(name)
            .description(description)
            // force location to null until we have an implementation of a workspace-wide location
            .location(null);
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    CloneWorkspaceResult initialResult =
        callWithRetries(
            () -> workspaceApi.cloneWorkspace(cloneRequest, workspaceId),
            "Error cloning workspace");
    logger.debug("clone workspace initial result: {}", initialResult);

    // poll until the workspace clone completes.
    // TODO PF-745: return immediately and give some interface for checking on the job status
    //     and retrieving the result.
    CloneWorkspaceResult cloneWorkspaceResult =
        handleClientExceptions(
            () ->
                HttpUtils.pollWithRetries(
                    () ->
                        workspaceApi.getCloneWorkspaceResult(
                            initialResult.getWorkspace().getDestinationWorkspaceId(),
                            initialResult.getJobReport().getId()),
                    (result) -> isDone(result.getJobReport()),
                    WorkspaceManagerService::isRetryable,
                    // Retry for 30 minutes, as this involves creating a new context
                    /*maxCalls=*/ 30,
                    /*sleepDuration=*/ Duration.ofSeconds(60)),
            "Error in cloning workspace.");
    logger.debug("clone workspace polling result: {}", cloneWorkspaceResult);
    throwIfJobNotCompleted(
        cloneWorkspaceResult.getJobReport(), cloneWorkspaceResult.getErrorReport());
    return cloneWorkspaceResult;
  }

  /**
   * Call the Workspace Manager PATCH "/api/workspaces/v1/{id}/properties" endpoint to delete
   * properties in workspace.
   *
   * @param workspaceId the id of the workspace to delete properties
   * @param workspacePropertyKeys the property keys for deleting
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription deleteWorkspaceProperties(
      UUID workspaceId, List<String> workspacePropertyKeys) {
    callWithRetries(
        () ->
            new WorkspaceApi(apiClient)
                .deleteWorkspaceProperties(workspacePropertyKeys, workspaceId),
        "Error deleting workspace properties");
    return callWithRetries(
        () -> new WorkspaceApi(apiClient).getWorkspace(workspaceId, /*minimumHighestRole=*/ null),
        "Error getting the workspace after deleting properties");
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
    GrantRoleRequestBody grantRoleRequestBody = new GrantRoleRequestBody().memberEmail(userEmail);
    if (server.getSamInviteRequiresAdmin()) {
      // if inviting a user requires admin permissions, don't invite whenever a user is not found
      // instead, require the admin to explicitly invite someone
      callWithRetries(
          () -> new WorkspaceApi(apiClient).grantRole(grantRoleRequestBody, workspaceId, iamRole),
          "Error granting IAM role on workspace");
    } else {
      // - try to grant the user an iam role
      // - if this fails with a Bad Request error, it means the email is not found
      // - so try to invite the user first, then retry granting them an iam role
      callAndHandleOneTimeError(
          () -> new WorkspaceApi(apiClient).grantRole(grantRoleRequestBody, workspaceId, iamRole),
          (ex) -> isHttpStatusCode(ex, HttpStatusCodes.STATUS_CODE_BAD_REQUEST),
          () -> SamService.fromContext().inviteUser(userEmail),
          "Error granting IAM role on workspace.");
    }
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
    callWithRetries(
        () -> new WorkspaceApi(apiClient).removeRole(workspaceId, iamRole, userEmail),
        "Error removing IAM role on workspace");
  }

  /**
   * Call the Workspace Manager "/api/workspace/v1/{id}/roles" endpoint to get a list of roles and
   * their members.
   *
   * @param workspaceId the workspace to query
   * @return a list of roles and the users that have them
   */
  public RoleBindingList getRoles(UUID workspaceId) {
    return callWithRetries(
        () -> new WorkspaceApi(apiClient).getRoles(workspaceId),
        "Error fetching users and their IAM roles for workspace");
  }

  /**
   * Call the Workspace Manager "/api/workspaces/v1/{workspaceId}/folders" endpoint to get a list of
   * folders.
   *
   * @param workspaceId the workspace to query
   * @return a list of folders in this workspace
   */
  public ImmutableList<Folder> listFolders(UUID workspaceId) {
    FolderList result =
        callWithRetries(
            () -> new FolderApi(apiClient).listFolders(workspaceId),
            "Error fetching list of folders");

    List<Folder> allFolders = new ArrayList<>(result.getFolders());
    return ImmutableList.copyOf(allFolders);
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1/{workspaceId}/resources" endpoint, possibly
   * multiple times, to get a list of all resources (controlled and referenced) in the workspace.
   * Throw an exception if the number of resources in the workspace is greater than the specified
   * limit.
   *
   * @param workspaceId the workspace to query
   * @param limit the maximum number of resources to return
   * @return a list of resources
   * @throws SystemException if the number of resources in the workspace > the specified limit
   */
  public List<ResourceDescription> enumerateAllResources(UUID workspaceId, int limit) {
    return handleClientExceptions(
        () -> {
          // poll the enumerate endpoint until no results are returned, or we hit the limit
          List<ResourceDescription> allResources = new ArrayList<>();
          int numResultsReturned;
          do {
            int offset = allResources.size();
            ResourceList result =
                HttpUtils.callWithRetries(
                    () ->
                        new ResourceApi(apiClient)
                            .enumerateResources(
                                workspaceId,
                                offset,
                                MAX_RESOURCES_PER_ENUMERATE_REQUEST,
                                null,
                                null),
                    WorkspaceManagerService::isRetryable);

            // add all fetched resources to the running list
            numResultsReturned = result.getResources().size();
            logger.debug("Called enumerate endpoints, fetched {} resources", numResultsReturned);
            allResources.addAll(result.getResources());

            // if we have fetched more than the limit, then throw an exception
            if (allResources.size() > limit) {
              throw new SystemException(
                  "Total number of resources ("
                      + allResources.size()
                      + ") exceeds the CLI limit ("
                      + limit
                      + ")");
            }

            // if this fetch returned less than the maximum allowed per request, then that indicates
            // there are no more
          } while (numResultsReturned >= MAX_RESOURCES_PER_ENUMERATE_REQUEST);

          logger.debug("Fetched total number of resources: {}", allResources.size());
          return allResources;
        },
        "Error enumerating resources in the workspace.");
  }

  /**
   * Call the Workspace Manager
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/{resourceId}/access" endpoint to check
   * if the current user has access to the referenced resource.
   *
   * @param workspaceId the workspace that contains the resource
   * @param resourceId the resource id
   * @return true if access is allowed
   */
  public boolean checkAccess(UUID workspaceId, UUID resourceId) {
    return callWithRetries(
        () -> new ResourceApi(apiClient).checkReferenceAccess(workspaceId, resourceId),
        "Error checking access to resource.");
  }

  protected ReferenceResourceCommonFields getReferencedResourceMetadata(
      CreateResourceParams resourceFields) {
    return new ReferenceResourceCommonFields()
        .name(resourceFields.name)
        .description(resourceFields.description)
        .cloningInstructions(resourceFields.cloningInstructions);
  }

  /**
   * Call the workspace Manager POST "/api/workspace/v1/{workspaceId}/resources/referenced/gitrepos"
   * endpoint to add a reference to a git repository in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param addGitRepoParams git repo referenced resource definition
   */
  public GitRepoResource createReferencedGitRepo(
      UUID workspaceId, AddGitRepoParams addGitRepoParams) {
    CreateGitRepoReferenceRequestBody createGitRepoReferenceRequestBody =
        new CreateGitRepoReferenceRequestBody()
            .metadata(getReferencedResourceMetadata(addGitRepoParams.resourceFields))
            .gitrepo(new GitRepoAttributes().gitRepoUrl(addGitRepoParams.gitRepoUrl));
    return callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .createGitRepoReference(createGitRepoReferenceRequestBody, workspaceId),
        "Error when creating a git repo referenced resource in the workspace");
  }

  /**
   * Call the Workspace Manager PATCH
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gitrepos/{resourceId}" endpoint to
   * update a Git repository referenced resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateReferencedGitRepo(
      UUID workspaceId, UUID resourceId, UpdateReferencedGitRepoParams updateParams) {
    UpdateGitRepoReferenceRequestBody updateGitRepoReferenceRequestBody =
        new UpdateGitRepoReferenceRequestBody()
            .name(updateParams.resourceFields.name)
            .description(updateParams.resourceFields.description)
            .gitRepoUrl(updateParams.gitRepoUrl)
            .cloningInstructions(updateParams.cloningInstructions);
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .updateGitRepoReference(updateGitRepoReferenceRequestBody, workspaceId, resourceId),
        "Error updating referenced Git repo in the workspace.");
  }

  /**
   * Call the Workspace Manager DELETE
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gitrepos/{resourceId}" endpoint to
   * delete a git repository as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   */
  public void deleteReferencedGitRepo(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient).deleteGitRepoReference(workspaceId, resourceId),
        "Error deleting referenced git repo in the workspace.");
  }

  /**
   * Execute a function that includes hitting WSM endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the WSM client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with no return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  protected void callWithRetries(
      HttpUtils.RunnableWithCheckedException<ApiException> makeRequest, String errorMsg) {
    handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, WorkspaceManagerService::isRetryable),
        errorMsg);
  }

  /**
   * Execute a function that includes hitting WSM endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the WSM client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  protected <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, WorkspaceManagerService::isRetryable),
        errorMsg);
  }

  /**
   * Execute a function, and possibly a second function to handle a one-time error, that includes
   * hitting WSM endpoints. Retry if the function throws an {@link #isRetryable} exception. If an
   * exception is thrown by the WSM client or the retries, make sure the HTTP status code and error
   * message are logged.
   *
   * @param makeRequest function with no return value
   * @param isOneTimeError function to test whether the exception is the expected one-time error
   * @param handleOneTimeError function to handle the one-time error before retrying the request
   * @param errorMsg error message for the {@link SystemException} that wraps any exceptions thrown
   *     by the SAM client or the retries
   */
  private void callAndHandleOneTimeError(
      HttpUtils.RunnableWithCheckedException<ApiException> makeRequest,
      Predicate<Exception> isOneTimeError,
      HttpUtils.RunnableWithCheckedException<ApiException> handleOneTimeError,
      String errorMsg) {
    handleClientExceptions(
        () ->
            HttpUtils.callAndHandleOneTimeErrorWithRetries(
                makeRequest,
                WorkspaceManagerService::isRetryable,
                isOneTimeError,
                handleOneTimeError,
                (ex) ->
                    false), // don't retry because the handleOneTimeError already includes retries
        errorMsg);
  }

  /**
   * Execute a function that includes hitting WSM endpoints. If an exception is thrown by the WSM
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with no return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  private void handleClientExceptions(
      HttpUtils.RunnableWithCheckedException<ApiException> makeRequest, String errorMsg) {
    handleClientExceptions(
        () -> {
          makeRequest.run();
          return null;
        },
        errorMsg);
  }

  /**
   * Execute a function that includes hitting WSM endpoints. If an exception is thrown by the WSM
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  protected <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (ApiException | InterruptedException ex) {
      // if this is a WSM client exception, check for a message in the response body
      if (ex instanceof ApiException) {
        errorMsg += ": " + logErrorMessage((ApiException) ex);
      }

      // wrap the WSM exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }

  /**
   * Helper method to convert a local date (e.g. 2014-01-02) into an object that includes time and
   * zone. The time is set to midnight, the zone to UTC.
   *
   * @param localDate date object with no time or zone/offset information included
   * @return object that specifies the date, time and zone/offest
   */
  public static OffsetDateTime dateAtMidnightAndUTC(@Nullable LocalDate localDate) {
    return localDate == null
        ? null
        : OffsetDateTime.of(localDate.atTime(LocalTime.MIDNIGHT), ZoneOffset.UTC);
  }

  /**
   * Create a common fields WSM object from a Resource that is being used to create a controlled
   * resource.
   */
  public static ControlledResourceCommonFields createCommonFields(
      CreateResourceParams createParams) {
    return new ControlledResourceCommonFields()
        .name(createParams.name)
        .description(createParams.description)
        .cloningInstructions(createParams.cloningInstructions)
        .accessScope(createParams.accessScope)
        .managedBy(ManagedBy.USER);
  }

  /** Helper method that checks a JobReport's status and returns false if it's still RUNNING. */
  public static boolean isDone(JobReport jobReport) {
    return !jobReport.getStatus().equals(JobReport.StatusEnum.RUNNING);
  }

  /**
   * Helper method that checks a JobReport's status and throws an exception if it's not COMPLETED.
   *
   * <p>- Throws a {@link SystemException} if the job FAILED.
   *
   * <p>- Throws a {@link UserActionableException} if the job is still RUNNING. Some actions are
   * expected to take a long time (e.g. deleting a bucket with lots of objects), and a timeout is
   * not necessarily a failure. The action the user can take is to wait a bit longer and then check
   * back (e.g. by listing the buckets in the workspace) later to see if the job completed.
   *
   * @param jobReport WSM job report object
   * @param errorReport WSM error report object
   */
  public static void throwIfJobNotCompleted(JobReport jobReport, ErrorReport errorReport) {
    switch (jobReport.getStatus()) {
      case FAILED -> throw new SystemException("Job failed: " + errorReport.getMessage());
      case RUNNING -> throw new UserActionableException(
          "CLI timed out waiting for the job to complete. It's still running on the server.");
    }
  }

  /**
   * Utility method that checks if an exception thrown by the WSM client matches the given HTTP
   * status code.
   *
   * @param ex exception to test
   * @return true if the exception status code matches
   */
  public static boolean isHttpStatusCode(Exception ex, int statusCode) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    int exceptionStatusCode = ((ApiException) ex).getCode();
    return statusCode == exceptionStatusCode;
  }

  /**
   * Utility method that checks if an exception thrown by the WSM client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  public static boolean isRetryable(Exception ex) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    logErrorMessage((ApiException) ex);
    int statusCode = ((ApiException) ex).getCode();
    // if a request to WSM times out, the client will wrap a SocketException in an ApiException,
    // set the HTTP status code to 0, and rethrows it to the caller. Unfortunately this is a
    // different exception than the SocketTimeoutException thrown by other client libraries.
    final int TIMEOUT_STATUS_CODE = 0;
    boolean isWsmTimeout =
        statusCode == TIMEOUT_STATUS_CODE && ex.getCause() instanceof SocketException;

    return isWsmTimeout
        || statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
        || statusCode == HttpStatus.SC_BAD_GATEWAY
        || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE
        || statusCode == HttpStatus.SC_GATEWAY_TIMEOUT;
  }

  /** Pull a human-readable error message from an ApiException. */
  public static String logErrorMessage(ApiException apiEx) {
    logger.error(
        "WSM exception status code: {}, response body: {}, message: {}",
        apiEx.getCode(),
        apiEx.getResponseBody(),
        apiEx.getMessage());

    // try to deserialize the response body into an ErrorReport
    String apiExMsg = apiEx.getResponseBody();
    if (apiExMsg != null)
      try {
        ErrorReport errorReport =
            JacksonMapper.getMapper().readValue(apiEx.getResponseBody(), ErrorReport.class);
        apiExMsg = errorReport.getMessage();
      } catch (JsonProcessingException jsonEx) {
        logger.debug("Error deserializing WSM exception ErrorReport: {}", apiEx.getResponseBody());
      }

    // if we found a SAM error message, then return it
    // otherwise return a string with the http code
    return ((apiExMsg != null && !apiExMsg.isEmpty())
        ? apiExMsg
        : apiEx.getCode() + " " + apiEx.getMessage());
  }

  public static List<Property> buildProperties(@Nullable Map<String, String> propertyMap) {
    if (propertyMap == null) {
      return new ArrayList<>();
    }
    return propertyMap.entrySet().stream()
        .map(
            entry -> {
              Property property = new Property();
              property.setKey(entry.getKey());
              property.setValue(entry.getValue());
              return property;
            })
        .collect(Collectors.toList());
  }
}
