package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.AddBqTableParams;
import bio.terra.cli.serialization.userfacing.input.AddGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.AddGitRepoParams;
import bio.terra.cli.serialization.userfacing.input.CreateBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.CreateGcpNotebookParams;
import bio.terra.cli.serialization.userfacing.input.CreateGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.cli.serialization.userfacing.input.GcsBucketLifecycle;
import bio.terra.cli.serialization.userfacing.input.GcsStorageClass;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcpNotebookParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedBqTableParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGitRepoParams;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.utils.JacksonMapper;
import bio.terra.workspace.api.ControlledGcpResourceApi;
import bio.terra.workspace.api.ReferencedGcpResourceApi;
import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.api.UnauthenticatedApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.CloneWorkspaceRequest;
import bio.terra.workspace.model.CloneWorkspaceResult;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.CloudPlatform;
import bio.terra.workspace.model.ControlledResourceCommonFields;
import bio.terra.workspace.model.CreateCloudContextRequest;
import bio.terra.workspace.model.CreateCloudContextResult;
import bio.terra.workspace.model.CreateControlledGcpAiNotebookInstanceRequestBody;
import bio.terra.workspace.model.CreateControlledGcpBigQueryDatasetRequestBody;
import bio.terra.workspace.model.CreateControlledGcpGcsBucketRequestBody;
import bio.terra.workspace.model.CreateGcpBigQueryDataTableReferenceRequestBody;
import bio.terra.workspace.model.CreateGcpBigQueryDatasetReferenceRequestBody;
import bio.terra.workspace.model.CreateGcpGcsBucketReferenceRequestBody;
import bio.terra.workspace.model.CreateGcpGcsObjectReferenceRequestBody;
import bio.terra.workspace.model.CreateGitRepoReferenceRequestBody;
import bio.terra.workspace.model.CreateTerraWorkspaceReferenceRequestBody;
import bio.terra.workspace.model.CreateWorkspaceRequestBody;
import bio.terra.workspace.model.CreatedControlledGcpAiNotebookInstanceResult;
import bio.terra.workspace.model.DeleteControlledGcpAiNotebookInstanceRequest;
import bio.terra.workspace.model.DeleteControlledGcpAiNotebookInstanceResult;
import bio.terra.workspace.model.DeleteControlledGcpGcsBucketRequest;
import bio.terra.workspace.model.DeleteControlledGcpGcsBucketResult;
import bio.terra.workspace.model.ErrorReport;
import bio.terra.workspace.model.GcpAiNotebookInstanceAcceleratorConfig;
import bio.terra.workspace.model.GcpAiNotebookInstanceContainerImage;
import bio.terra.workspace.model.GcpAiNotebookInstanceCreationParameters;
import bio.terra.workspace.model.GcpAiNotebookInstanceResource;
import bio.terra.workspace.model.GcpAiNotebookInstanceVmImage;
import bio.terra.workspace.model.GcpAiNotebookUpdateParameters;
import bio.terra.workspace.model.GcpBigQueryDataTableAttributes;
import bio.terra.workspace.model.GcpBigQueryDataTableResource;
import bio.terra.workspace.model.GcpBigQueryDatasetAttributes;
import bio.terra.workspace.model.GcpBigQueryDatasetCreationParameters;
import bio.terra.workspace.model.GcpBigQueryDatasetResource;
import bio.terra.workspace.model.GcpBigQueryDatasetUpdateParameters;
import bio.terra.workspace.model.GcpGcsBucketAttributes;
import bio.terra.workspace.model.GcpGcsBucketCreationParameters;
import bio.terra.workspace.model.GcpGcsBucketLifecycle;
import bio.terra.workspace.model.GcpGcsBucketLifecycleRule;
import bio.terra.workspace.model.GcpGcsBucketLifecycleRuleAction;
import bio.terra.workspace.model.GcpGcsBucketLifecycleRuleCondition;
import bio.terra.workspace.model.GcpGcsBucketResource;
import bio.terra.workspace.model.GcpGcsBucketUpdateParameters;
import bio.terra.workspace.model.GcpGcsObjectAttributes;
import bio.terra.workspace.model.GcpGcsObjectResource;
import bio.terra.workspace.model.GitRepoAttributes;
import bio.terra.workspace.model.GitRepoResource;
import bio.terra.workspace.model.GrantRoleRequestBody;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.JobControl;
import bio.terra.workspace.model.JobReport;
import bio.terra.workspace.model.JobReport.StatusEnum;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.ReferenceResourceCommonFields;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceList;
import bio.terra.workspace.model.RoleBindingList;
import bio.terra.workspace.model.SystemVersion;
import bio.terra.workspace.model.TerraWorkspaceAttributes;
import bio.terra.workspace.model.TerraWorkspaceResource;
import bio.terra.workspace.model.UpdateBigQueryDataTableReferenceRequestBody;
import bio.terra.workspace.model.UpdateBigQueryDatasetReferenceRequestBody;
import bio.terra.workspace.model.UpdateControlledGcpAiNotebookInstanceRequestBody;
import bio.terra.workspace.model.UpdateControlledGcpBigQueryDatasetRequestBody;
import bio.terra.workspace.model.UpdateControlledGcpGcsBucketRequestBody;
import bio.terra.workspace.model.UpdateGcsBucketObjectReferenceRequestBody;
import bio.terra.workspace.model.UpdateGcsBucketReferenceRequestBody;
import bio.terra.workspace.model.UpdateGitRepoReferenceRequestBody;
import bio.terra.workspace.model.UpdateWorkspaceRequestBody;
import bio.terra.workspace.model.WorkspaceDescription;
import bio.terra.workspace.model.WorkspaceDescriptionList;
import bio.terra.workspace.model.WorkspaceStageModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
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
  private static final int CLONE_WORKSPACE_MAXIMUM_RETRIES = 360;
  private static final Duration CLONE_WORKSPACE_RETRY_INTERVAL = Duration.ofSeconds(10);

  // the Terra environment where the WSM service lives
  private final Server server;

  // the client object used for talking to WSM
  private final ApiClient apiClient;

  // the maximum number of retries and time to sleep for creating a new workspace
  private static final int CREATE_WORKSPACE_MAXIMUM_RETRIES = 120;
  private static final Duration CREATE_WORKSPACE_DURATION_SLEEP_FOR_RETRY = Duration.ofSeconds(1);

  // maximum number of resources to fetch per call to the enumerate endpoint
  private static final int MAX_RESOURCES_PER_ENUMERATE_REQUEST = 100;

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
   * Constructor for class that talks to WSM. If the access token is null, only unauthenticated
   * endpoints can be called.
   */
  private WorkspaceManagerService(@Nullable AccessToken accessToken, Server server) {
    this.server = server;
    this.apiClient = new ApiClient();

    this.apiClient.setBasePath(server.getWorkspaceManagerUri());
    if (accessToken != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      this.apiClient.setAccessToken(accessToken.getTokenValue());
    }
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
        () -> new WorkspaceApi(apiClient).listWorkspaces(offset, limit),
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
   * @return the Workspace Manager workspace description object
   * @throws SystemException if the job to create the workspace cloud context fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public WorkspaceDescription createWorkspace(
      String userFacingId,
      @Nullable String name,
      @Nullable String description,
      @Nullable Map<String, String> properties) {
    return handleClientExceptions(
        () -> {
          // create the Terra workspace object
          UUID workspaceId = UUID.randomUUID();
          CreateWorkspaceRequestBody workspaceRequestBody = new CreateWorkspaceRequestBody();
          workspaceRequestBody.setId(workspaceId);
          workspaceRequestBody.setUserFacingId(userFacingId);
          workspaceRequestBody.setStage(WorkspaceStageModel.MC_WORKSPACE);
          workspaceRequestBody.setSpendProfile(Context.getServer().getWsmDefaultSpendProfile());
          workspaceRequestBody.setDisplayName(name);
          workspaceRequestBody.setDescription(description);
          workspaceRequestBody.setProperties(Workspace.stringMapToProperties(properties));

          // make the create workspace request
          WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
          HttpUtils.callWithRetries(
              () -> workspaceApi.createWorkspace(workspaceRequestBody),
              WorkspaceManagerService::isRetryable);

          // create the Google project that backs the Terra workspace object
          UUID jobId = UUID.randomUUID();
          CreateCloudContextRequest cloudContextRequest = new CreateCloudContextRequest();
          cloudContextRequest.setCloudPlatform(CloudPlatform.GCP);
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
                  CREATE_WORKSPACE_MAXIMUM_RETRIES,
                  CREATE_WORKSPACE_DURATION_SLEEP_FOR_RETRY);
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
            // if this is a spend profile access denied error, then throw a more user-friendly error
            // message
            if (createContextResult.getErrorReport().getMessage().contains("spend profile")
                && createContextResult.getErrorReport().getStatusCode()
                    == HttpStatusCodes.STATUS_CODE_FORBIDDEN) {
              final String errorMessage;
              if (workspaceSuccessfullyDeleted) {
                errorMessage =
                    "Accessing the spend profile failed. Ask an administrator to grant you access.";
              } else {
                errorMessage =
                    String.format(
                        "Accessing the spend profile failed. Ask an administrator to grant you access. "
                            + "There was a problem cleaning up the partially created workspace ID %s",
                        workspaceId);
              }
              throw new UserActionableException(errorMessage);
            }
          }
          // handle non-spend-profile-related failures
          throwIfJobNotCompleted(
              createContextResult.getJobReport(), createContextResult.getErrorReport());

          // call the get workspace endpoint to get the full description object
          return HttpUtils.callWithRetries(
              () -> workspaceApi.getWorkspace(workspaceId), WorkspaceManagerService::isRetryable);
        },
        "Error creating a new workspace");
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1/{id}" endpoint to fetch an existing
   * workspace.
   */
  public WorkspaceDescription getWorkspace(UUID uuid, boolean isDataCollectionWorkspace) {
    WorkspaceDescription workspaceWithContext =
        callWithRetries(
            () -> new WorkspaceApi(apiClient).getWorkspace(uuid),
            "Error fetching workspace",
            isDataCollectionWorkspace);
    String googleProjectId =
        (workspaceWithContext.getGcpContext() == null)
            ? null
            : workspaceWithContext.getGcpContext().getProjectId();
    logger.info(
        "Workspace context: userFacingId {}, project id: {}",
        workspaceWithContext.getUserFacingId(),
        googleProjectId);
    return workspaceWithContext;
  }

  /**
   * Call the Workspace Manager GET "/api/workspaces/v1/workspaceByUserFacingId/{userFacingId}"
   * endpoint to fetch an existing workspace.
   */
  public WorkspaceDescription getWorkspaceByUserFacingId(String userFacingId) {
    WorkspaceDescription workspaceWithContext =
        callWithRetries(
            () -> new WorkspaceApi(apiClient).getWorkspaceByUserFacingId(userFacingId),
            "Error fetching workspace");
    String googleProjectId =
        (workspaceWithContext.getGcpContext() == null)
            ? null
            : workspaceWithContext.getGcpContext().getProjectId();
    logger.info(
        "Workspace context: {}, project id: {}",
        workspaceWithContext.getUserFacingId(),
        googleProjectId);
    return workspaceWithContext;
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
   * Call the Workspace Manager POST "/api/workspaces/v1/{id}/gcp/enablepet" endpoint to grant the
   * currently logged in user and their pet permission to impersonate their own pet service account
   * in a workspace.
   *
   * @param workspaceId the id of the workspace to enable pet impersonation in
   * @return the email identifier of the pet SA which the user can now impersonate
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
   * @return object with information about the clone job success and destination workspace
   */
  public CloneWorkspaceResult cloneWorkspace(
      UUID workspaceId, String userFacingId, @Nullable String name, @Nullable String description) {
    var request =
        new CloneWorkspaceRequest()
            .spendProfile(Context.getServer().getWsmDefaultSpendProfile())
            .userFacingId(userFacingId)
            .displayName(name)
            .description(description)
            // force location to null until we have an implementation of a workspace-wide location
            .location(null);
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    CloneWorkspaceResult initialResult =
        callWithRetries(
            () -> workspaceApi.cloneWorkspace(request, workspaceId), "Error cloning workspace");
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
                    CLONE_WORKSPACE_MAXIMUM_RETRIES,
                    CLONE_WORKSPACE_RETRY_INTERVAL),
            "Error in cloning workspace.");
    logger.debug("clone workspace polling result: {}", cloneWorkspaceResult);
    throwIfJobNotCompleted(
        cloneWorkspaceResult.getJobReport(), cloneWorkspaceResult.getErrorReport());
    return cloneWorkspaceResult;
  }

  /**
   * Call the Workspace Manager PATCH "/api/workspaces/v1/{id}/properties" endpoint to update
   * properties in workspace.
   *
   * @param workspaceId the id of the workspace to update
   * @param workspacePropertyKeys the update properties
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
        () -> new WorkspaceApi(apiClient).getWorkspace(workspaceId),
        "Error getting the workspace after updating properties");
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
          int numResultsReturned = 0;
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

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bucket/objects" endpoint to add a
   * GCS bucket file as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams creation parameters
   * @return the GCS bucket file resource object
   */
  public GcpGcsObjectResource createReferencedGcsObject(
      UUID workspaceId, AddGcsObjectParams createParams) {
    // convert the CLI object to a WSM request object
    CreateGcpGcsObjectReferenceRequestBody createRequest =
        new CreateGcpGcsObjectReferenceRequestBody()
            .metadata(getReferencedResourceMetadata(createParams.resourceFields))
            .file(
                new GcpGcsObjectAttributes()
                    .bucketName(createParams.bucketName)
                    .fileName(createParams.objectName));
    return callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .createGcsObjectReference(createRequest, workspaceId),
        "Error creating referenced GCS bucket file in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/buckets" endpoint to add a GCS
   * bucket as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams creation parameters
   * @return the GCS bucket resource object
   */
  public GcpGcsBucketResource createReferencedGcsBucket(
      UUID workspaceId, CreateGcsBucketParams createParams) {
    // convert the CLI object to a WSM request object
    CreateGcpGcsBucketReferenceRequestBody createRequest =
        new CreateGcpGcsBucketReferenceRequestBody()
            .metadata(getReferencedResourceMetadata(createParams.resourceFields))
            .bucket(new GcpGcsBucketAttributes().bucketName(createParams.bucketName));
    return callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .createBucketReference(createRequest, workspaceId),
        "Error creating referenced GCS bucket in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatatables" endpoint to add
   * a BigQuery data table as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams resource definition to add
   * @return the BigQuery data table resource object
   */
  public GcpBigQueryDataTableResource createReferencedBigQueryDataTable(
      UUID workspaceId, AddBqTableParams createParams) {
    // convert the CLI object to a WSM request object
    CreateGcpBigQueryDataTableReferenceRequestBody createRequest =
        new CreateGcpBigQueryDataTableReferenceRequestBody()
            .metadata(getReferencedResourceMetadata(createParams.resourceFields))
            .dataTable(
                new GcpBigQueryDataTableAttributes()
                    .projectId(createParams.projectId)
                    .datasetId(createParams.datasetId)
                    .dataTableId(createParams.dataTableId));
    return callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .createBigQueryDataTableReference(createRequest, workspaceId),
        "Error creating referenced BigQuery data table in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatasets" endpoint to add a
   * BigQuery dataset as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams resource definition to add
   * @return the BigQuery dataset resource object
   */
  public GcpBigQueryDatasetResource createReferencedBigQueryDataset(
      UUID workspaceId, CreateBqDatasetParams createParams) {
    // convert the CLI object to a WSM request object
    CreateGcpBigQueryDatasetReferenceRequestBody createRequest =
        new CreateGcpBigQueryDatasetReferenceRequestBody()
            .metadata(getReferencedResourceMetadata(createParams.resourceFields))
            .dataset(
                new GcpBigQueryDatasetAttributes()
                    .projectId(createParams.projectId)
                    .datasetId(createParams.datasetId));
    return callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .createBigQueryDatasetReference(createRequest, workspaceId),
        "Error creating referenced BigQuery dataset in the workspace.");
  }

  private ReferenceResourceCommonFields getReferencedResourceMetadata(
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
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/ai-notebook-instance" endpoint to
   * add a GCP notebook instance as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams resource definition to create
   * @return the GCP notebook instance resource object
   */
  public GcpAiNotebookInstanceResource createControlledGcpNotebookInstance(
      UUID workspaceId, CreateGcpNotebookParams createParams) {
    // convert the CLI object to a WSM request object
    String jobId = UUID.randomUUID().toString();
    CreateControlledGcpAiNotebookInstanceRequestBody createRequest =
        new CreateControlledGcpAiNotebookInstanceRequestBody()
            .common(createCommonFields(createParams.resourceFields))
            .aiNotebookInstance(fromCLIObject(createParams))
            .jobControl(new JobControl().id(jobId));
    logger.debug("Create controlled GCP notebook request {}", createRequest);

    return handleClientExceptions(
        () -> {
          ControlledGcpResourceApi controlledGcpResourceApi =
              new ControlledGcpResourceApi(apiClient);
          // Start the GCP notebook creation job.
          HttpUtils.callWithRetries(
              () -> controlledGcpResourceApi.createAiNotebookInstance(createRequest, workspaceId),
              WorkspaceManagerService::isRetryable);

          // Poll the result endpoint until the job is no longer RUNNING.
          CreatedControlledGcpAiNotebookInstanceResult createResult =
              HttpUtils.pollWithRetries(
                  () ->
                      controlledGcpResourceApi.getCreateAiNotebookInstanceResult(
                          workspaceId, jobId),
                  (result) -> isDone(result.getJobReport()),
                  WorkspaceManagerService::isRetryable,
                  // Creating a GCP notebook instance should take less than ~10 minutes.
                  60,
                  Duration.ofSeconds(10));
          logger.debug("Create controlled GCP notebook result {}", createResult);
          throwIfJobNotCompleted(createResult.getJobReport(), createResult.getErrorReport());
          return createResult.getAiNotebookInstance();
        },
        "Error creating controlled GCP Notebook instance in the workspace.");
  }

  /**
   * This method converts this CLI-defined POJO class into the WSM client library-defined request
   * object.
   *
   * @return GCP notebook attributes in the format expected by the WSM client library
   */
  private static GcpAiNotebookInstanceCreationParameters fromCLIObject(
      CreateGcpNotebookParams createParams) {
    GcpAiNotebookInstanceCreationParameters notebookParams =
        new GcpAiNotebookInstanceCreationParameters()
            .instanceId(createParams.instanceId)
            .location(createParams.location)
            .machineType(createParams.machineType)
            .postStartupScript(createParams.postStartupScript)
            .metadata(createParams.metadata)
            .installGpuDriver(createParams.installGpuDriver)
            .customGpuDriverPath(createParams.customGpuDriverPath)
            .bootDiskType(createParams.bootDiskType)
            .bootDiskSizeGb(createParams.bootDiskSizeGb)
            .dataDiskType(createParams.dataDiskType)
            .dataDiskSizeGb(createParams.dataDiskSizeGb);
    if (createParams.acceleratorType != null || createParams.acceleratorCoreCount != null) {
      notebookParams.acceleratorConfig(
          new GcpAiNotebookInstanceAcceleratorConfig()
              .type(createParams.acceleratorType)
              .coreCount(createParams.acceleratorCoreCount));
    }
    if (createParams.vmImageProject != null) {
      notebookParams.vmImage(
          new GcpAiNotebookInstanceVmImage()
              .projectId(createParams.vmImageProject)
              .imageFamily(createParams.vmImageFamily)
              .imageName(createParams.vmImageName));
    } else if (createParams.containerRepository != null) {
      notebookParams.containerImage(
          new GcpAiNotebookInstanceContainerImage()
              .repository(createParams.containerRepository)
              .tag(createParams.containerTag));
    } else {
      throw new SystemException("Expected either VM or Container image definition.");
    }
    return notebookParams;
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/buckets" endpoint to add a GCS
   * bucket as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams creation parameters
   * @return the GCS bucket resource object
   */
  public GcpGcsBucketResource createControlledGcsBucket(
      UUID workspaceId, CreateGcsBucketParams createParams) {
    // convert the CLI lifecycle rule object into the WSM request objects
    List<GcpGcsBucketLifecycleRule> lifecycleRules = fromCLIObject(createParams.lifecycle);

    // convert the CLI object to a WSM request object
    CreateControlledGcpGcsBucketRequestBody createRequest =
        new CreateControlledGcpGcsBucketRequestBody()
            .common(createCommonFields(createParams.resourceFields))
            .gcsBucket(
                new GcpGcsBucketCreationParameters()
                    .name(createParams.bucketName)
                    .defaultStorageClass(createParams.defaultStorageClass)
                    .lifecycle(new GcpGcsBucketLifecycle().rules(lifecycleRules))
                    .location(createParams.location));
    return callWithRetries(
        () ->
            new ControlledGcpResourceApi(apiClient)
                .createBucket(createRequest, workspaceId)
                .getGcpBucket(),
        "Error creating controlled GCS bucket in the workspace.");
  }

  /**
   * This method converts this CLI-defined POJO class into a list of WSM client library-defined
   * request objects.
   *
   * @return list of lifecycle rules in the format expected by the WSM client library
   */
  private static List<GcpGcsBucketLifecycleRule> fromCLIObject(GcsBucketLifecycle lifecycle) {
    List<GcpGcsBucketLifecycleRule> wsmLifecycleRules = new ArrayList<>();
    for (GcsBucketLifecycle.Rule rule : lifecycle.rule) {
      GcpGcsBucketLifecycleRuleAction action =
          new GcpGcsBucketLifecycleRuleAction().type(rule.action.type.toWSMEnum());
      if (rule.action.storageClass != null) {
        action.storageClass(rule.action.storageClass.toWSMEnum());
      }

      GcpGcsBucketLifecycleRuleCondition condition =
          new GcpGcsBucketLifecycleRuleCondition()
              .age(rule.condition.age)
              .createdBefore(dateAtMidnightAndUTC(rule.condition.createdBefore))
              .customTimeBefore(dateAtMidnightAndUTC(rule.condition.customTimeBefore))
              .daysSinceCustomTime(rule.condition.daysSinceCustomTime)
              .daysSinceNoncurrentTime(rule.condition.daysSinceNoncurrentTime)
              .live(rule.condition.isLive)
              .matchesStorageClass(
                  rule.condition.matchesStorageClass.stream()
                      .map(GcsStorageClass::toWSMEnum)
                      .collect(Collectors.toList()))
              .noncurrentTimeBefore(dateAtMidnightAndUTC(rule.condition.noncurrentTimeBefore))
              .numNewerVersions(rule.condition.numNewerVersions);

      GcpGcsBucketLifecycleRule lifecycleRuleRequestObject =
          new GcpGcsBucketLifecycleRule().action(action).condition(condition);
      wsmLifecycleRules.add(lifecycleRuleRequestObject);
    }
    return wsmLifecycleRules;
  }

  /**
   * Helper method to convert a local date (e.g. 2014-01-02) into an object that includes time and
   * zone. The time is set to midnight, the zone to UTC.
   *
   * @param localDate date object with no time or zone/offset information included
   * @return object that specifies the date, time and zone/offest
   */
  private static OffsetDateTime dateAtMidnightAndUTC(@Nullable LocalDate localDate) {
    return localDate == null
        ? null
        : OffsetDateTime.of(localDate.atTime(LocalTime.MIDNIGHT), ZoneOffset.UTC);
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/bqdatasets" endpoint to add a Big
   * Query dataset as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams resource definition to create
   * @return the BigQuery dataset resource object
   */
  public GcpBigQueryDatasetResource createControlledBigQueryDataset(
      UUID workspaceId, CreateBqDatasetParams createParams) {
    // convert the CLI object to a WSM request object
    CreateControlledGcpBigQueryDatasetRequestBody createRequest =
        new CreateControlledGcpBigQueryDatasetRequestBody()
            .common(createCommonFields(createParams.resourceFields))
            .dataset(
                new GcpBigQueryDatasetCreationParameters()
                    .datasetId(createParams.datasetId)
                    .location(createParams.location)
                    .defaultPartitionLifetime(createParams.defaultPartitionLifetimeSeconds)
                    .defaultTableLifetime(createParams.defaultTableLifetimeSeconds));
    return callWithRetries(
        () ->
            new ControlledGcpResourceApi(apiClient)
                .createBigQueryDataset(createRequest, workspaceId)
                .getBigQueryDataset(),
        "Error creating controlled BigQuery dataset in the workspace.");
  }

  /** Used by tests only. */
  public TerraWorkspaceResource createReferencedTerraWorkspace(
      UUID workspaceId, UUID referencedWorkspaceId, String referenceName) {
    CreateTerraWorkspaceReferenceRequestBody createRequest =
        new CreateTerraWorkspaceReferenceRequestBody()
            .metadata(
                new ReferenceResourceCommonFields()
                    .name(referenceName)
                    .cloningInstructions(CloningInstructionsEnum.NOTHING))
            .referencedWorkspace(
                new TerraWorkspaceAttributes().referencedWorkspaceId(referencedWorkspaceId));
    return callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .createTerraWorkspaceReference(createRequest, workspaceId),
        "Error creating referenced data collection in the workspace.");
  }

  /**
   * Create a common fields WSM object from a Resource that is being used to create a controlled
   * resource.
   */
  private static ControlledResourceCommonFields createCommonFields(
      CreateResourceParams createParams) {
    ControlledResourceCommonFields commonFields =
        new ControlledResourceCommonFields()
            .name(createParams.name)
            .description(createParams.description)
            .cloningInstructions(createParams.cloningInstructions)
            .accessScope(createParams.accessScope)
            .managedBy(ManagedBy.USER);

    return commonFields;
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bucket/objects/{resourceId}"
   * endpoint to update a GCS bucket object referenced resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateReferencedGcsObject(
      UUID workspaceId, UUID resourceId, UpdateReferencedGcsObjectParams updateParams) {
    // convert the CLI object to a WSM request object
    UpdateGcsBucketObjectReferenceRequestBody updateRequest =
        new UpdateGcsBucketObjectReferenceRequestBody();
    updateRequest
        .name(updateParams.resourceFields.name)
        .description(updateParams.resourceFields.description)
        .bucketName(updateParams.bucketName)
        .objectName(updateParams.objectName)
        .cloningInstructions(updateParams.cloningInstructions);

    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .updateBucketObjectReferenceResource(updateRequest, workspaceId, resourceId),
        "Error updating referenced GCS bucket object in the workspace.");
  }

  /**
   * Call the Workspace Manager PATCH
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/buckets/{resourceId}" endpoint to
   * update a GCS bucket referenced resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateReferencedGcsBucket(
      UUID workspaceId, UUID resourceId, UpdateReferencedGcsBucketParams updateParams) {
    // convert the CLI object to a WSM request object
    UpdateGcsBucketReferenceRequestBody updateRequest =
        new UpdateGcsBucketReferenceRequestBody()
            .name(updateParams.resourceParams.name)
            .description(updateParams.resourceParams.description)
            .bucketName(updateParams.bucketName)
            .cloningInstructions(updateParams.cloningInstructions);
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .updateBucketReferenceResource(updateRequest, workspaceId, resourceId),
        "Error updating referenced GCS bucket in the workspace.");
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
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/buckets/{resourceId}" endpoint to
   * update a GCS bucket controlled resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateControlledGcsBucket(
      UUID workspaceId, UUID resourceId, UpdateControlledGcsBucketParams updateParams) {
    // convert the CLI lifecycle rule object into the WSM request objects
    List<GcpGcsBucketLifecycleRule> lifecycleRules = fromCLIObject(updateParams.lifecycle);

    // convert the CLI object to a WSM request object
    UpdateControlledGcpGcsBucketRequestBody updateRequest =
        new UpdateControlledGcpGcsBucketRequestBody()
            .name(updateParams.resourceFields.name)
            .description(updateParams.resourceFields.description)
            .updateParameters(
                new GcpGcsBucketUpdateParameters()
                    .defaultStorageClass(updateParams.defaultStorageClass)
                    .lifecycle(new GcpGcsBucketLifecycle().rules(lifecycleRules))
                    .cloningInstructions(updateParams.cloningInstructions));
    callWithRetries(
        () ->
            new ControlledGcpResourceApi(apiClient)
                .updateGcsBucket(updateRequest, workspaceId, resourceId),
        "Error updating controlled GCS bucket in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/notebooks/{resourceId}" endpoint to
   * update a GCP Notebook controlled resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateControlledGcpNotebook(
      UUID workspaceId, UUID resourceId, UpdateControlledGcpNotebookParams updateParams) {

    // convert the CLI object to a WSM request object
    UpdateControlledGcpAiNotebookInstanceRequestBody updateRequest =
        new UpdateControlledGcpAiNotebookInstanceRequestBody()
            .name(updateParams.resourceFields.name)
            .description(updateParams.resourceFields.description);
    if (updateParams.notebookUpdateParameters != null) {
      updateRequest.updateParameters(
          new GcpAiNotebookUpdateParameters()
              .metadata(updateParams.notebookUpdateParameters.getMetadata()));
    }
    callWithRetries(
        () ->
            new ControlledGcpResourceApi(apiClient)
                .updateAiNotebookInstance(updateRequest, workspaceId, resourceId),
        "Error updating controlled GCP notebook in the workspace.");
  }

  /**
   * Call the Workspace Manager PATCH
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatatables/{resourceId}"
   * endpoint to update a BigQuery data table referenced resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateReferencedBigQueryDataTable(
      UUID workspaceId, UUID resourceId, UpdateReferencedBqTableParams updateParams) {
    // convert the CLI object to a WSM request object
    UpdateBigQueryDataTableReferenceRequestBody updateRequest =
        new UpdateBigQueryDataTableReferenceRequestBody()
            .name(updateParams.resourceParams.name)
            .description(updateParams.resourceParams.description)
            .projectId(updateParams.projectId)
            .datasetId(updateParams.datasetId)
            .dataTableId(updateParams.tableId)
            .cloningInstructions(updateParams.cloningInstructions);

    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .updateBigQueryDataTableReferenceResource(updateRequest, workspaceId, resourceId),
        "Error updating referenced BigQuery data table in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatasets/{resourceId}"
   * endpoint to update a BigQuery dataset referenced resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateReferencedBigQueryDataset(
      UUID workspaceId, UUID resourceId, UpdateReferencedBqDatasetParams updateParams) {
    // convert the CLI object to a WSM request object
    UpdateBigQueryDatasetReferenceRequestBody updateRequest =
        new UpdateBigQueryDatasetReferenceRequestBody()
            .name(updateParams.resourceParams.name)
            .description(updateParams.resourceParams.description)
            .projectId(updateParams.projectId)
            .datasetId(updateParams.datasetId)
            .cloningInstructions(updateParams.cloningInstructions);
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .updateBigQueryDatasetReferenceResource(updateRequest, workspaceId, resourceId),
        "Error updating referenced BigQuery dataset in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/bqdatasets/{resourceId}" endpoint to
   * update a BigQuery dataset controlled resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateControlledBigQueryDataset(
      UUID workspaceId, UUID resourceId, UpdateControlledBqDatasetParams updateParams) {

    // convert the CLI object to a WSM request object
    UpdateControlledGcpBigQueryDatasetRequestBody updateRequest =
        new UpdateControlledGcpBigQueryDatasetRequestBody()
            .name(updateParams.resourceFields.name)
            .description(updateParams.resourceFields.description)
            .updateParameters(
                new GcpBigQueryDatasetUpdateParameters()
                    .defaultPartitionLifetime(updateParams.defaultPartitionLifetimeSeconds)
                    .defaultTableLifetime(updateParams.defaultTableLifetimeSeconds)
                    .cloningInstructions(updateParams.cloningInstructions));
    callWithRetries(
        () ->
            new ControlledGcpResourceApi(apiClient)
                .updateBigQueryDataset(updateRequest, workspaceId, resourceId),
        "Error updating controlled BigQuery dataset in the workspace.");
  }

  /**
   * Call the Workspace Manager DELETE
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bucket/objects/{resourceId}"
   * endpoint to delete a GCS bucket object as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   */
  public void deleteReferencedGcsObject(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .deleteGcsObjectReference(workspaceId, resourceId),
        "Error deleting referenced GCS bucket object in the workspace.");
  }

  /**
   * Call the Workspace Manager DELETE
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/buckets/{resourceId}" endpoint to
   * delete a GCS bucket as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   */
  public void deleteReferencedGcsBucket(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient).deleteBucketReference(workspaceId, resourceId),
        "Error deleting referenced GCS bucket in the workspace.");
  }

  /**
   * Call the Workspace Manager DELETE
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatatables/{resourceId}"
   * endpoint to delete a BigQuery data table as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   */
  public void deleteReferencedBigQueryDataTable(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .deleteBigQueryDataTableReference(workspaceId, resourceId),
        "Error deleting referenced BigQuery data table in the workspace.");
  }

  /**
   * Call the Workspace Manager DELETE
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatasets/{resourceId}"
   * endpoint to delete a BigQuery dataset as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   */
  public void deleteReferencedBigQueryDataset(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .deleteBigQueryDatasetReference(workspaceId, resourceId),
        "Error deleting referenced BigQuery dataset in the workspace.");
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

  public void deleteReferencedTerraWorkspace(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .deleteTerraWorkspaceReference(workspaceId, resourceId),
        "Error deleting referenced data collection in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/ai-notebook-instances/{resourceId}"
   * endpoint to delete a GCP notebook instance as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   * @throws SystemException if the job to delete the GCP notebook instance fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public void deleteControlledGcpNotebookInstance(UUID workspaceId, UUID resourceId) {
    ControlledGcpResourceApi controlledGcpResourceApi = new ControlledGcpResourceApi(apiClient);
    String asyncJobId = UUID.randomUUID().toString();
    var deleteRequest =
        new DeleteControlledGcpAiNotebookInstanceRequest()
            .jobControl(new JobControl().id(asyncJobId));
    handleClientExceptions(
        () -> {
          // make the initial delete request
          HttpUtils.callWithRetries(
              () ->
                  controlledGcpResourceApi.deleteAiNotebookInstance(
                      deleteRequest, workspaceId, resourceId),
              WorkspaceManagerService::isRetryable);

          // poll the result endpoint until the job is no longer RUNNING
          DeleteControlledGcpAiNotebookInstanceResult deleteResult =
              HttpUtils.pollWithRetries(
                  () ->
                      controlledGcpResourceApi.getDeleteAiNotebookInstanceResult(
                          workspaceId, asyncJobId),
                  (result) -> isDone(result.getJobReport()),
                  WorkspaceManagerService::isRetryable);
          logger.debug("delete controlled GCP notebook instance result: {}", deleteResult);

          throwIfJobNotCompleted(deleteResult.getJobReport(), deleteResult.getErrorReport());
        },
        "Error deleting controlled GCP Notebook instance in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/buckets/{resourceId}" endpoint to
   * delete a GCS bucket as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   * @throws SystemException if the job to delete the bucket fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public void deleteControlledGcsBucket(UUID workspaceId, UUID resourceId) {
    ControlledGcpResourceApi controlledGcpResourceApi = new ControlledGcpResourceApi(apiClient);
    String asyncJobId = UUID.randomUUID().toString();
    DeleteControlledGcpGcsBucketRequest deleteRequest =
        new DeleteControlledGcpGcsBucketRequest().jobControl(new JobControl().id(asyncJobId));
    handleClientExceptions(
        () -> {
          // make the initial delete request
          HttpUtils.callWithRetries(
              () -> controlledGcpResourceApi.deleteBucket(deleteRequest, workspaceId, resourceId),
              WorkspaceManagerService::isRetryable);

          // poll the result endpoint until the job is no longer RUNNING
          DeleteControlledGcpGcsBucketResult deleteResult =
              HttpUtils.pollWithRetries(
                  () -> controlledGcpResourceApi.getDeleteBucketResult(workspaceId, asyncJobId),
                  (result) -> isDone(result.getJobReport()),
                  WorkspaceManagerService::isRetryable);
          logger.debug("delete controlled gcs bucket result: {}", deleteResult);

          throwIfJobNotCompleted(deleteResult.getJobReport(), deleteResult.getErrorReport());
        },
        "Error deleting controlled GCS bucket in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/bqdatasets/{resourceId}" endpoint to
   * delete a BigQuery dataset as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   */
  public void deleteControlledBigQueryDataset(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ControlledGcpResourceApi(apiClient).deleteBigQueryDataset(workspaceId, resourceId),
        "Error deleting controlled BigQuery dataset in the workspace.");
  }

  /** Helper method that checks a JobReport's status and returns false if it's still RUNNING. */
  private static boolean isDone(JobReport jobReport) {
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
  private static void throwIfJobNotCompleted(JobReport jobReport, ErrorReport errorReport) {
    switch (jobReport.getStatus()) {
      case FAILED:
        throw new SystemException("Job failed: " + errorReport.getMessage());
      case RUNNING:
        throw new UserActionableException(
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
  private static boolean isHttpStatusCode(Exception ex, int statusCode) {
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
  private static boolean isRetryable(Exception ex) {
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

  /**
   * Execute a function that includes hitting WSM endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the WSM client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with no return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  private void callWithRetries(
      HttpUtils.RunnableWithCheckedException<ApiException> makeRequest, String errorMsg) {
    handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, WorkspaceManagerService::isRetryable),
        errorMsg);
  }

  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    return callWithRetries(makeRequest, errorMsg, /*isDataCollectionWorkspace=*/ false);
  }

  /**
   * Execute a function that includes hitting WSM endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the WSM client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   * @param isDataCollectionWorkspace whether workspace is data collection workspace
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest,
      String errorMsg,
      boolean isDataCollectionWorkspace) {
    return handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, WorkspaceManagerService::isRetryable),
        errorMsg,
        isDataCollectionWorkspace);
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

  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    return handleClientExceptions(makeRequest, errorMsg, /*isDataCollectionWorkspace=*/ false);
  }

  /**
   * Execute a function that includes hitting WSM endpoints. If an exception is thrown by the WSM
   * client or the retries, make sure the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest,
      String errorMsg,
      boolean isDataCollectionWorkspace) {
    try {
      return makeRequest.makeRequest();
    } catch (ApiException | InterruptedException ex) {
      // if this is a WSM client exception, check for a message in the response body
      if (ex instanceof ApiException) {

        // If requester doesn't have access to data collection workspace, normally
        // `terra resource describe` would return:
        //     [ERROR] Error fetching workspace: User XXX is not authorized to read resource YYY of
        //     type workspace
        // We generally hide the fact that data collections are workspaces. Show a different error
        // that doesn't have "workspace".
        if (isDataCollectionWorkspace) {
          throw new SystemException("User does not have access to this data collection", ex);
        }

        String exceptionErrorMessage = logErrorMessage((ApiException) ex);

        errorMsg += ": " + exceptionErrorMessage;
      }

      // wrap the WSM exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }

  /** Pull a human-readable error message from an ApiException. */
  private static String logErrorMessage(ApiException apiEx) {
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
}
