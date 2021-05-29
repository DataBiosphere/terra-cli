package bio.terra.cli.service.utils;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Resource;
import bio.terra.cli.context.Server;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.context.resources.GcsBucket;
import bio.terra.cli.context.resources.GcsBucketLifecycle;
import bio.terra.cli.context.resources.GcsStorageClass;
import bio.terra.workspace.api.ControlledGcpResourceApi;
import bio.terra.workspace.api.ReferencedGcpResourceApi;
import bio.terra.workspace.api.ResourceApi;
import bio.terra.workspace.api.UnauthenticatedApi;
import bio.terra.workspace.api.WorkspaceApi;
import bio.terra.workspace.client.ApiClient;
import bio.terra.workspace.client.ApiException;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
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
import bio.terra.workspace.model.GcpAiNotebookInstanceCreationParameters;
import bio.terra.workspace.model.GcpAiNotebookInstanceResource;
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
import bio.terra.workspace.model.SystemStatus;
import bio.terra.workspace.model.SystemVersion;
import bio.terra.workspace.model.UpdateWorkspaceRequestBody;
import bio.terra.workspace.model.WorkspaceDescription;
import bio.terra.workspace.model.WorkspaceDescriptionList;
import bio.terra.workspace.model.WorkspaceStageModel;
import com.google.api.client.http.HttpStatusCodes;
import com.google.auth.oauth2.AccessToken;
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
   * Constructor for class that talks to the Workspace Manager service. The user must be
   * authenticated. Methods in this class will use its credentials to call authenticated endpoints.
   */
  public WorkspaceManagerService(Server server, TerraUser terraUser) {
    this.apiClient = new ApiClient();
    // check that there is a current user, we will use their credentials to communicate with WSM
    buildClientForTerraUser(server, terraUser);
  }

  /**
   * Constructor for class that talks to the Workspace Manager service. The user must be
   * authenticated. Methods in this class will use its credentials to call authenticated endpoints.
   */
  public WorkspaceManagerService() {
    this(GlobalContext.get().server, GlobalContext.get().requireCurrentTerraUser());
  }

  /**
   * Build the Workspace Manager API client object for the given Terra user and global context. If
   * terraUser is null, this method returns the client object without an access token set.
   *
   * @param server the Terra environment where the Workspace Manager service lives
   * @param terraUser the Terra user whose credentials will be used to call authenticated endpoints
   */
  private void buildClientForTerraUser(Server server, TerraUser terraUser) {
    this.apiClient.setBasePath(server.workspaceManagerUri);

    if (terraUser != null) {
      // fetch the user access token
      // this method call will attempt to refresh the token if it's already expired
      AccessToken userAccessToken = terraUser.getUserAccessToken();
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
   * @return the Workspace Manager workspace list object
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
   * @throws SystemException if the job to create the workspace cloud context fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public WorkspaceDescription createWorkspace(
      @Nullable String displayName, @Nullable String description) {
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

      // make the initial create context request
      workspaceApi.createCloudContext(cloudContextRequest, workspaceId);

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
      UUID workspaceId, @Nullable String displayName, @Nullable String description) {
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
    GlobalContext globalContext = GlobalContext.get();
    WorkspaceApi workspaceApi = new WorkspaceApi(apiClient);
    GrantRoleRequestBody grantRoleRequestBody = new GrantRoleRequestBody().memberEmail(userEmail);
    try {
      // - try to grant the user an iam role
      // - if this fails with a Bad Request error, it means the email is not found
      // - so try to invite the user first, then retry granting them an iam role
      HttpUtils.callAndHandleOneTimeError(
          () -> {
            workspaceApi.grantRole(grantRoleRequestBody, workspaceId, iamRole);
            return null;
          },
          WorkspaceManagerService::isBadRequest,
          () -> {
            new SamService(globalContext.server, globalContext.requireCurrentTerraUser())
                .inviteUser(userEmail);
            return null;
          });
    } catch (Exception secondEx) {
      throw new SystemException("Error granting IAM role on workspace.", secondEx);
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
    ResourceApi resourceApi = new ResourceApi(apiClient);
    try {
      // poll the enumerate endpoint until no results are returned, or we hit the limit
      List<ResourceDescription> allResources = new ArrayList<>();
      int numResultsReturned = 0;
      do {
        int offset = allResources.size();
        ResourceList result =
            resourceApi.enumerateResources(
                workspaceId, offset, MAX_RESOURCES_PER_ENUMERATE_REQUEST, null, null);

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
    } catch (ApiException ex) {
      throw new SystemException("Error enumerating resources in the workspace.", ex);
    }
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/buckets" endpoint to add a GCS
   * bucket as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param resourceToAdd resource to add
   * @return the GCS bucket resource object
   */
  public GcpGcsBucketResource createReferencedGcsBucket(UUID workspaceId, GcsBucket resourceToAdd) {
    // convert the GcsBucket object to a CreateGcpGcsBucketReferenceRequestBody object
    CreateGcpGcsBucketReferenceRequestBody createRequest =
        new CreateGcpGcsBucketReferenceRequestBody()
            .metadata(
                new ReferenceResourceCommonFields()
                    .name(resourceToAdd.name)
                    .description(resourceToAdd.description)
                    .cloningInstructions(resourceToAdd.cloningInstructions))
            .bucket(new GcpGcsBucketAttributes().bucketName(resourceToAdd.bucketName));

    try {
      ReferencedGcpResourceApi referencedGcpResourceApi = new ReferencedGcpResourceApi(apiClient);
      return referencedGcpResourceApi.createBucketReference(createRequest, workspaceId);
    } catch (ApiException ex) {
      throw new SystemException("Error creating referenced GCS bucket in the workspace.", ex);
    }
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/referenced/gcp/bigquerydatasets" endpoint to add a
   * Big Query dataset as a referenced resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param resourceToAdd resource definition to add
   * @return the Big Query dataset resource object
   */
  public GcpBigQueryDatasetResource createReferencedBigQueryDataset(
      UUID workspaceId, ResourceDescription resourceToAdd) {
    // convert the ResourceDescription object to a CreateGcpBigQueryDatasetReferenceRequestBody
    // object
    String name = resourceToAdd.getMetadata().getName();
    String description = resourceToAdd.getMetadata().getDescription();
    CloningInstructionsEnum cloningInstructions =
        resourceToAdd.getMetadata().getCloningInstructions();
    String gcpProjectId = resourceToAdd.getResourceAttributes().getGcpBqDataset().getProjectId();
    String bigQueryDatasetId =
        resourceToAdd.getResourceAttributes().getGcpBqDataset().getDatasetId();

    CreateGcpBigQueryDatasetReferenceRequestBody createRequest =
        new CreateGcpBigQueryDatasetReferenceRequestBody()
            .metadata(
                new ReferenceResourceCommonFields()
                    .name(name)
                    .description(description)
                    .cloningInstructions(cloningInstructions))
            .dataset(
                new GcpBigQueryDatasetAttributes()
                    .projectId(gcpProjectId)
                    .datasetId(bigQueryDatasetId));

    try {
      ReferencedGcpResourceApi referencedGcpResourceApi = new ReferencedGcpResourceApi(apiClient);
      return referencedGcpResourceApi.createBigQueryDatasetReference(createRequest, workspaceId);
    } catch (ApiException ex) {
      throw new SystemException(
          "Error creating referenced Big Query dataset in the workspace.", ex);
    }
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/ai-notebook-instance" endpoint to
   * add an AI Platform Notebook instance as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param resourceToCreate resource definition to create
   * @return the AI Platform Notebook instance resource object
   */
  public GcpAiNotebookInstanceResource createControlledAiNotebookInstance(
      UUID workspaceId,
      ResourceDescription resourceToCreate,
      GcpAiNotebookInstanceCreationParameters creationParameters) {
    String jobId = UUID.randomUUID().toString();
    CreateControlledGcpAiNotebookInstanceRequestBody createRequest =
        new CreateControlledGcpAiNotebookInstanceRequestBody()
            .common(createCommonFields(resourceToCreate))
            .aiNotebookInstance(creationParameters)
            .jobControl(new JobControl().id(jobId));

    try {
      ControlledGcpResourceApi controlledGcpResourceApi = new ControlledGcpResourceApi(apiClient);
      // Start the AI notebook creation job.
      controlledGcpResourceApi.createAiNotebookInstance(createRequest, workspaceId);
      // Poll the result endpoint until the job is no longer RUNNING.
      CreatedControlledGcpAiNotebookInstanceResult createResult =
          HttpUtils.pollWithRetries(
              () -> controlledGcpResourceApi.getCreateAiNotebookInstanceResult(workspaceId, jobId),
              (result) -> isDone(result.getJobReport()),
              WorkspaceManagerService::isRetryable,
              // Creating an AI notebook instance should take less than ~10 minutes.
              60,
              Duration.ofSeconds(10));
      logger.debug("Create controlled AI notebook result {}", createResult);
      throwIfJobNotCompleted(createResult.getJobReport(), createResult.getErrorReport());
      return createResult.getAiNotebookInstance();
    } catch (ApiException | InterruptedException ex) {
      throw new SystemException(
          "Error creating controlled AI Notebook instance in the workspace.", ex);
    }
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/buckets" endpoint to add a GCS
   * bucket as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param resourceToCreate resource definition to create
   * @return the GCS bucket resource object
   */
  public GcpGcsBucketResource createControlledGcsBucket(
      UUID workspaceId, GcsBucket resourceToCreate) {
    // convert the CLI lifecycle rule object into the WSM request objects
    List<GcpGcsBucketLifecycleRule> lifecycleRules = fromCLIObject(resourceToCreate.lifecycle);

    // convert the ResourceDescription object to a CreateControlledGcpGcsBucketRequestBody object
    CreateControlledGcpGcsBucketRequestBody createRequest =
        new CreateControlledGcpGcsBucketRequestBody()
            .common(createCommonFields(resourceToCreate))
            .gcsBucket(
                new GcpGcsBucketCreationParameters()
                    .name(resourceToCreate.bucketName)
                    .defaultStorageClass(resourceToCreate.defaultStorageClass)
                    .lifecycle(new GcpGcsBucketLifecycle().rules(lifecycleRules))
                    .location(resourceToCreate.location));

    try {
      ControlledGcpResourceApi controlledGcpResourceApi = new ControlledGcpResourceApi(apiClient);
      return controlledGcpResourceApi.createBucket(createRequest, workspaceId).getGcpBucket();
    } catch (ApiException ex) {
      throw new SystemException("Error creating controlled GCS bucket in the workspace.", ex);
    }
  }

  /**
   * This method converts this CLI-defined POJO class into a list of WSM client library-defined
   * request objects.
   *
   * @return list of lifecycle rules in the format expected by the WSM client library
   */
  public static List<GcpGcsBucketLifecycleRule> fromCLIObject(GcsBucketLifecycle lifecycle) {
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
   * @param resourceToCreate resource definition to create
   * @param location Big Query dataset location (https://cloud.google.com/bigquery/docs/locations)
   * @return the Big Query dataset resource object
   */
  public GcpBigQueryDatasetResource createControlledBigQueryDataset(
      UUID workspaceId, ResourceDescription resourceToCreate, @Nullable String location) {
    // convert the ResourceDescription object to a CreateControlledGcpBigQueryDatasetRequestBody
    // object
    String bigQueryDatasetId =
        resourceToCreate.getResourceAttributes().getGcpBqDataset().getDatasetId();

    CreateControlledGcpBigQueryDatasetRequestBody createRequest =
        new CreateControlledGcpBigQueryDatasetRequestBody()
            .common(createCommonFields(resourceToCreate))
            .dataset(
                new GcpBigQueryDatasetCreationParameters()
                    .datasetId(bigQueryDatasetId)
                    .location(location));

    try {
      ControlledGcpResourceApi controlledGcpResourceApi = new ControlledGcpResourceApi(apiClient);
      return controlledGcpResourceApi
          .createBigQueryDataset(createRequest, workspaceId)
          .getBigQueryDataset();
    } catch (ApiException ex) {
      throw new SystemException(
          "Error creating controlled Big Query dataset in the workspace.", ex);
    }
  }

  /**
   * Create a common fields object from a ResourceDescription that is being used to create a
   * controlled resource.
   */
  private static ControlledResourceCommonFields createCommonFields(
      ResourceDescription resourceToCreate) {
    String name = resourceToCreate.getMetadata().getName();
    String description = resourceToCreate.getMetadata().getDescription();
    CloningInstructionsEnum cloningInstructions =
        resourceToCreate.getMetadata().getCloningInstructions();
    AccessScope accessScope =
        resourceToCreate.getMetadata().getControlledResourceMetadata().getAccessScope();
    String privateUserEmail =
        resourceToCreate
            .getMetadata()
            .getControlledResourceMetadata()
            .getPrivateResourceUser()
            .getUserName();
    PrivateResourceIamRoles privateResourceIamRoles =
        resourceToCreate
            .getMetadata()
            .getControlledResourceMetadata()
            .getPrivateResourceUser()
            .getPrivateResourceIamRoles();

    return new ControlledResourceCommonFields()
        .name(name)
        .description(description)
        .cloningInstructions(cloningInstructions)
        .accessScope(accessScope)
        .privateResourceUser(
            new PrivateResourceUser()
                .userName(privateUserEmail)
                .privateResourceIamRoles(privateResourceIamRoles))
        .managedBy(ManagedBy.USER);
  }

  /**
   * Create a common fields WSM object from a Resource that is being used to create a controlled
   * resource.
   */
  private static ControlledResourceCommonFields createCommonFields(Resource resourceToCreate) {
    String name = resourceToCreate.name;
    String description = resourceToCreate.description;
    CloningInstructionsEnum cloningInstructions = resourceToCreate.cloningInstructions;
    AccessScope accessScope = resourceToCreate.accessScope;
    String privateUserEmail = resourceToCreate.privateUserName;

    PrivateResourceIamRoles privateResourceIamRoles = new PrivateResourceIamRoles();
    if (resourceToCreate.privateUserRoles != null) {
      privateResourceIamRoles.addAll(resourceToCreate.privateUserRoles);
    }

    return new ControlledResourceCommonFields()
        .name(name)
        .description(description)
        .cloningInstructions(cloningInstructions)
        .accessScope(accessScope)
        .privateResourceUser(
            new PrivateResourceUser()
                .userName(privateUserEmail)
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
    ReferencedGcpResourceApi referencedGcpResourceApi = new ReferencedGcpResourceApi(apiClient);
    try {
      referencedGcpResourceApi.deleteBucketReference(workspaceId, resourceId);
    } catch (ApiException ex) {
      throw new SystemException("Error deleting referenced GCS bucket in the workspace.", ex);
    }
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
    ReferencedGcpResourceApi referencedGcpResourceApi = new ReferencedGcpResourceApi(apiClient);
    try {
      referencedGcpResourceApi.deleteBigQueryDatasetReference(workspaceId, resourceId);
    } catch (ApiException ex) {
      throw new SystemException(
          "Error deleting referenced Big Query dataset in the workspace.", ex);
    }
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
    try {
      // make the initial delete request
      controlledGcpResourceApi.deleteAiNotebookInstance(deleteRequest, workspaceId, resourceId);

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
    } catch (ApiException | InterruptedException ex) {
      throw new SystemException(
          "Error deleting controlled AI Notebook instance in the workspace.", ex);
    }
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
    try {
      // make the initial delete request
      controlledGcpResourceApi.deleteBucket(deleteRequest, workspaceId, resourceId);

      // poll the result endpoint until the job is no longer RUNNING
      DeleteControlledGcpGcsBucketResult deleteResult =
          HttpUtils.pollWithRetries(
              () -> controlledGcpResourceApi.getDeleteBucketResult(workspaceId, asyncJobId),
              (result) -> isDone(result.getJobReport()),
              WorkspaceManagerService::isRetryable);
      logger.debug("delete controlled gcs bucket result: {}", deleteResult);

      throwIfJobNotCompleted(deleteResult.getJobReport(), deleteResult.getErrorReport());
    } catch (ApiException | InterruptedException ex) {
      throw new SystemException("Error deleting controlled GCS bucket in the workspace.", ex);
    }
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
    ControlledGcpResourceApi controlledGcpResourceApi = new ControlledGcpResourceApi(apiClient);
    try {
      controlledGcpResourceApi.deleteBigQueryDataset(workspaceId, resourceId);
    } catch (ApiException ex) {
      throw new SystemException(
          "Error deleting controlled Big Query dataset in the workspace.", ex);
    }
  }

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
   * Utility method that checks if an exception thrown by the WSM client is retryable.
   *
   * @param ex exception to test
   * @return true if the exception is retryable
   */
  private static boolean isRetryable(Exception ex) {
    if (!(ex instanceof ApiException)) {
      return false;
    }
    // TODO (PF-513): add retries for WSM calls after confirm what codes should be retryable
    return false;
  }
}
