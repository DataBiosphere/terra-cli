package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateAiNotebook;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateBqDataset;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateGcsBucket;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateResource;
import bio.terra.cli.serialization.userfacing.inputs.GcsBucketLifecycle;
import bio.terra.cli.serialization.userfacing.inputs.GcsStorageClass;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cli.utils.JacksonMapper;
import bio.terra.workspace.api.ControlledGcpResourceApi;
import bio.terra.workspace.api.ReferencedGcpResourceApi;
import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.api.UnauthenticatedApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.CloudPlatform;
import bio.terra.workspace.model.ControlledResourceCommonFields;
import bio.terra.workspace.model.CreateCloudContextRequest;
import bio.terra.workspace.model.CreateCloudContextResult;
import bio.terra.workspace.model.CreateControlledGcpAiNotebookInstanceRequestBody;
import bio.terra.workspace.model.CreateControlledGcpBigQueryDatasetRequestBody;
import bio.terra.workspace.model.CreateControlledGcpGcsBucketRequestBody;
import bio.terra.workspace.model.CreateGcpBigQueryDatasetReferenceRequestBody;
import bio.terra.workspace.model.CreateGcpGcsBucketReferenceRequestBody;
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
import bio.terra.workspace.model.GcpBigQueryDatasetAttributes;
import bio.terra.workspace.model.GcpBigQueryDatasetCreationParameters;
import bio.terra.workspace.model.GcpBigQueryDatasetResource;
import bio.terra.workspace.model.GcpGcsBucketAttributes;
import bio.terra.workspace.model.GcpGcsBucketCreationParameters;
import bio.terra.workspace.model.GcpGcsBucketLifecycle;
import bio.terra.workspace.model.GcpGcsBucketLifecycleRule;
import bio.terra.workspace.model.GcpGcsBucketLifecycleRuleAction;
import bio.terra.workspace.model.GcpGcsBucketLifecycleRuleCondition;
import bio.terra.workspace.model.GcpGcsBucketResource;
import bio.terra.workspace.model.GrantRoleRequestBody;
import bio.terra.workspace.model.IamRole;
import bio.terra.workspace.model.JobControl;
import bio.terra.workspace.model.JobReport;
import bio.terra.workspace.model.ManagedBy;
import bio.terra.workspace.model.PrivateResourceIamRoles;
import bio.terra.workspace.model.PrivateResourceUser;
import bio.terra.workspace.model.ReferenceResourceCommonFields;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceList;
import bio.terra.workspace.model.RoleBindingList;
import bio.terra.workspace.model.SystemVersion;
import bio.terra.workspace.model.UpdateWorkspaceRequestBody;
import bio.terra.workspace.model.WorkspaceDescription;
import bio.terra.workspace.model.WorkspaceDescriptionList;
import bio.terra.workspace.model.WorkspaceStageModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Workspace Manager endpoints. */
public class WorkspaceManagerService {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerService.class);

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
    return new WorkspaceManagerService(
        Context.requireUser().getUserAccessToken(), Context.getServer());
  }

  /**
   * Constructor for class that talks to WSM. If the access token is null, only unauthenticated
   * endpoints can be called.
   */
  public WorkspaceManagerService(@Nullable AccessToken accessToken, Server server) {
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
   * @param displayName optional display name
   * @param description optional description
   * @return the Workspace Manager workspace description object
   * @throws SystemException if the job to create the workspace cloud context fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public WorkspaceDescription createWorkspace(
      @Nullable String displayName, @Nullable String description) {
    return handleClientExceptions(
        () -> {
          // create the Terra workspace object
          UUID workspaceId = UUID.randomUUID();
          CreateWorkspaceRequestBody workspaceRequestBody = new CreateWorkspaceRequestBody();
          workspaceRequestBody.setId(workspaceId);
          workspaceRequestBody.setStage(WorkspaceStageModel.MC_WORKSPACE);
          workspaceRequestBody.setSpendProfile("wm-default-spend-profile");
          workspaceRequestBody.setDisplayName(displayName);
          workspaceRequestBody.setDescription(description);

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
   *
   * @param workspaceId the id of the workspace to fetch
   * @return the Workspace Manager workspace description object
   */
  public WorkspaceDescription getWorkspace(UUID workspaceId) {
    WorkspaceDescription workspaceWithContext =
        callWithRetries(
            () -> new WorkspaceApi(apiClient).getWorkspace(workspaceId),
            "Error fetching workspace");
    String googleProjectId =
        (workspaceWithContext.getGcpContext() == null)
            ? null
            : workspaceWithContext.getGcpContext().getProjectId();
    logger.info(
        "Workspace context: {}, project id: {}", workspaceWithContext.getId(), googleProjectId);
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
      UUID workspaceId, @Nullable String displayName, @Nullable String description) {
    UpdateWorkspaceRequestBody updateRequest =
        new UpdateWorkspaceRequestBody().displayName(displayName).description(description);
    return callWithRetries(
        () -> new WorkspaceApi(apiClient).updateWorkspace(updateRequest, workspaceId),
        "Error updating workspace");
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
    handleClientExceptions(
        () -> {
          // - try to grant the user an iam role
          // - if this fails with a Bad Request error, it means the email is not found
          // - so try to invite the user first, then retry granting them an iam role
          HttpUtils.callAndHandleOneTimeErrorWithRetries(
              () ->
                  new WorkspaceApi(apiClient).grantRole(grantRoleRequestBody, workspaceId, iamRole),
              WorkspaceManagerService::isRetryable,
              (ex) -> isStatusCode(ex, HttpStatusCodes.STATUS_CODE_BAD_REQUEST),
              () -> SamService.fromContext().inviteUser(userEmail),
              (ex) -> false); // don't retry because inviteUser already includes retries
        },
        "Error granting IAM role on workspace.");
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
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/buckets" endpoint to add a GCS
   * bucket as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams creation parameters
   * @return the GCS bucket resource object
   */
  public GcpGcsBucketResource createReferencedGcsBucket(
      UUID workspaceId, CreateUpdateGcsBucket createParams) {
    // convert the CLI object to a WSM request object
    CreateGcpGcsBucketReferenceRequestBody createRequest =
        new CreateGcpGcsBucketReferenceRequestBody()
            .metadata(
                new ReferenceResourceCommonFields()
                    .name(createParams.resourceFields.name)
                    .description(createParams.resourceFields.description)
                    .cloningInstructions(createParams.resourceFields.cloningInstructions))
            .bucket(new GcpGcsBucketAttributes().bucketName(createParams.bucketName));
    return callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .createBucketReference(createRequest, workspaceId),
        "Error creating referenced GCS bucket in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatasets" endpoint to add a
   * Big Query dataset as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams resource definition to add
   * @return the Big Query dataset resource object
   */
  public GcpBigQueryDatasetResource createReferencedBigQueryDataset(
      UUID workspaceId, CreateUpdateBqDataset createParams) {
    // convert the CLI object to a WSM request object
    CreateGcpBigQueryDatasetReferenceRequestBody createRequest =
        new CreateGcpBigQueryDatasetReferenceRequestBody()
            .metadata(
                new ReferenceResourceCommonFields()
                    .name(createParams.resourceFields.name)
                    .description(createParams.resourceFields.description)
                    .cloningInstructions(createParams.resourceFields.cloningInstructions))
            .dataset(
                new GcpBigQueryDatasetAttributes()
                    .projectId(createParams.projectId)
                    .datasetId(createParams.datasetId));
    return callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .createBigQueryDatasetReference(createRequest, workspaceId),
        "Error creating referenced Big Query dataset in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/ai-notebook-instance" endpoint to
   * add an AI Platform Notebook instance as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams resource definition to create
   * @return the AI Platform Notebook instance resource object
   */
  public GcpAiNotebookInstanceResource createControlledAiNotebookInstance(
      UUID workspaceId, CreateUpdateAiNotebook createParams) {
    // convert the CLI object to a WSM request object
    String jobId = UUID.randomUUID().toString();
    CreateControlledGcpAiNotebookInstanceRequestBody createRequest =
        new CreateControlledGcpAiNotebookInstanceRequestBody()
            .common(createCommonFields(createParams.resourceFields))
            .aiNotebookInstance(fromCLIObject(createParams))
            .jobControl(new JobControl().id(jobId));

    return handleClientExceptions(
        () -> {
          ControlledGcpResourceApi controlledGcpResourceApi =
              new ControlledGcpResourceApi(apiClient);
          // Start the AI notebook creation job.
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
                  // Creating an AI notebook instance should take less than ~10 minutes.
                  60,
                  Duration.ofSeconds(10));
          logger.debug("Create controlled AI notebook result {}", createResult);
          throwIfJobNotCompleted(createResult.getJobReport(), createResult.getErrorReport());
          return createResult.getAiNotebookInstance();
        },
        "Error creating controlled AI Notebook instance in the workspace.");
  }

  /**
   * This method converts this CLI-defined POJO class into the WSM client library-defined request
   * object.
   *
   * @return AI Platform notebook attributes in the format expected by the WSM client library
   */
  private static GcpAiNotebookInstanceCreationParameters fromCLIObject(
      CreateUpdateAiNotebook createParams) {
    GcpAiNotebookInstanceCreationParameters aiNotebookParams =
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
      aiNotebookParams.acceleratorConfig(
          new GcpAiNotebookInstanceAcceleratorConfig()
              .type(createParams.acceleratorType)
              .coreCount(createParams.acceleratorCoreCount));
    }
    if (createParams.vmImageProject != null) {
      aiNotebookParams.vmImage(
          new GcpAiNotebookInstanceVmImage()
              .projectId(createParams.vmImageProject)
              .imageFamily(createParams.vmImageFamily)
              .imageName(createParams.vmImageName));
    } else if (createParams.containerRepository != null) {
      aiNotebookParams.containerImage(
          new GcpAiNotebookInstanceContainerImage()
              .repository(createParams.containerRepository)
              .tag(createParams.containerTag));
    } else {
      throw new SystemException("Expected either VM or Container image definition.");
    }
    return aiNotebookParams;
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
      UUID workspaceId, CreateUpdateGcsBucket createParams) {
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
   * @return the Big Query dataset resource object
   */
  public GcpBigQueryDatasetResource createControlledBigQueryDataset(
      UUID workspaceId, CreateUpdateBqDataset createParams) {
    // convert the CLI object to a WSM request object
    CreateControlledGcpBigQueryDatasetRequestBody createRequest =
        new CreateControlledGcpBigQueryDatasetRequestBody()
            .common(createCommonFields(createParams.resourceFields))
            .dataset(
                new GcpBigQueryDatasetCreationParameters()
                    .datasetId(createParams.datasetId)
                    .location(createParams.location));
    return callWithRetries(
        () ->
            new ControlledGcpResourceApi(apiClient)
                .createBigQueryDataset(createRequest, workspaceId)
                .getBigQueryDataset(),
        "Error creating controlled Big Query dataset in the workspace.");
  }

  /**
   * Create a common fields WSM object from a Resource that is being used to create a controlled
   * resource.
   */
  private static ControlledResourceCommonFields createCommonFields(
      CreateUpdateResource createParams) {
    PrivateResourceIamRoles privateResourceIamRoles = new PrivateResourceIamRoles();
    if (createParams.privateUserRoles != null) {
      privateResourceIamRoles.addAll(createParams.privateUserRoles);
    }
    return new ControlledResourceCommonFields()
        .name(createParams.name)
        .description(createParams.description)
        .cloningInstructions(createParams.cloningInstructions)
        .accessScope(createParams.accessScope)
        .privateResourceUser(
            new PrivateResourceUser()
                .userName(createParams.privateUserName)
                .privateResourceIamRoles(privateResourceIamRoles))
        .managedBy(ManagedBy.USER);
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
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatasets/{resourceId}"
   * endpoint to delete a Big Query dataset as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   */
  public void deleteReferencedBigQueryDataset(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ReferencedGcpResourceApi(apiClient)
                .deleteBigQueryDatasetReference(workspaceId, resourceId),
        "Error deleting referenced Big Query dataset in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/ai-notebook-instances/{resourceId}"
   * endpoint to delete an AI notebook instance as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   * @throws SystemException if the job to delete the AI notebook instance fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public void deleteControlledAiNotebookInstance(UUID workspaceId, UUID resourceId) {
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
          logger.debug("delete controlled AI notebook instance result: {}", deleteResult);

          throwIfJobNotCompleted(deleteResult.getJobReport(), deleteResult.getErrorReport());
        },
        "Error deleting controlled AI Notebook instance in the workspace.");
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
   * delete a Big Query dataset as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   */
  public void deleteControlledBigQueryDataset(UUID workspaceId, UUID resourceId) {
    callWithRetries(
        () ->
            new ControlledGcpResourceApi(apiClient).deleteBigQueryDataset(workspaceId, resourceId),
        "Error deleting controlled Big Query dataset in the workspace.");
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
  private static boolean isStatusCode(Exception ex, int statusCode) {
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
    if (ex instanceof SocketTimeoutException) {
      return true;
    } else if (!(ex instanceof ApiException)) {
      return false;
    }
    int statusCode = ((ApiException) ex).getCode();
    return statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR
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

  /**
   * Execute a function that includes hitting WSM endpoints. Retry if the function throws an {@link
   * #isRetryable} exception. If an exception is thrown by the WSM client or the retries, make sure
   * the HTTP status code and error message are logged.
   *
   * @param makeRequest function with a return value
   * @param errorMsg error message for the the {@link SystemException} that wraps any exceptions
   *     thrown by the WSM client or the retries
   */
  private <T> T callWithRetries(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    return handleClientExceptions(
        () -> HttpUtils.callWithRetries(makeRequest, WorkspaceManagerService::isRetryable),
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
  private <T> T handleClientExceptions(
      HttpUtils.SupplierWithCheckedException<T, ApiException> makeRequest, String errorMsg) {
    try {
      return makeRequest.makeRequest();
    } catch (ApiException | InterruptedException ex) {
      // if this is a WSM client exception, check for a message in the response body
      if (ex instanceof ApiException) {
        ApiException apiEx = (ApiException) ex;
        logger.error(
            "WSM exception status code: {}, response body: {}, message: {}",
            apiEx.getCode(),
            apiEx.getResponseBody(),
            apiEx.getMessage());

        // try to deserialize the response body into an ErrorReport
        String apiExMsg;
        try {
          ErrorReport errorReport =
              JacksonMapper.getMapper().readValue(apiEx.getResponseBody(), ErrorReport.class);
          apiExMsg = errorReport.getMessage();
        } catch (JsonProcessingException jsonEx) {
          logger.debug(
              "Error deserializing WSM exception ErrorReport: {}", apiEx.getResponseBody());
          apiExMsg = apiEx.getResponseBody();
        }

        // if we found a WSM error message, then append it to the one passed in
        // otherwise append the http code
        errorMsg +=
            ": "
                + ((apiExMsg != null && !apiExMsg.isEmpty())
                    ? apiExMsg
                    : apiEx.getCode() + " " + apiEx.getMessage());
      }

      // wrap the WSM exception and re-throw it
      throw new SystemException(errorMsg, ex);
    }
  }
}
