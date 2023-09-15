package bio.terra.cli.service;

import static bio.terra.cli.serialization.userfacing.input.CreateGcpDataprocClusterParams.toWsmCreateClusterParams;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.AddBqTableParams;
import bio.terra.cli.serialization.userfacing.input.AddGcsObjectParams;
import bio.terra.cli.serialization.userfacing.input.CreateBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.CreateGcpDataprocClusterParams;
import bio.terra.cli.serialization.userfacing.input.CreateGcpNotebookParams;
import bio.terra.cli.serialization.userfacing.input.CreateGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.GcsBucketLifecycle;
import bio.terra.cli.serialization.userfacing.input.GcsStorageClass;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcpDataprocClusterParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcpNotebookParams;
import bio.terra.cli.serialization.userfacing.input.UpdateControlledGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedBqDatasetParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedBqTableParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsBucketParams;
import bio.terra.cli.serialization.userfacing.input.UpdateReferencedGcsObjectParams;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.workspace.api.ControlledGcpResourceApi;
import bio.terra.workspace.api.ReferencedGcpResourceApi;
import bio.terra.workspace.model.ControlledDataprocClusterUpdateParameters;
import bio.terra.workspace.model.CreateControlledGcpAiNotebookInstanceRequestBody;
import bio.terra.workspace.model.CreateControlledGcpBigQueryDatasetRequestBody;
import bio.terra.workspace.model.CreateControlledGcpDataprocClusterRequestBody;
import bio.terra.workspace.model.CreateControlledGcpGcsBucketRequestBody;
import bio.terra.workspace.model.CreateGcpBigQueryDataTableReferenceRequestBody;
import bio.terra.workspace.model.CreateGcpBigQueryDatasetReferenceRequestBody;
import bio.terra.workspace.model.CreateGcpGcsBucketReferenceRequestBody;
import bio.terra.workspace.model.CreateGcpGcsObjectReferenceRequestBody;
import bio.terra.workspace.model.CreatedControlledGcpAiNotebookInstanceResult;
import bio.terra.workspace.model.CreatedControlledGcpDataprocClusterResult;
import bio.terra.workspace.model.DeleteControlledGcpAiNotebookInstanceRequest;
import bio.terra.workspace.model.DeleteControlledGcpAiNotebookInstanceResult;
import bio.terra.workspace.model.DeleteControlledGcpDataprocClusterRequest;
import bio.terra.workspace.model.DeleteControlledGcpDataprocClusterResult;
import bio.terra.workspace.model.DeleteControlledGcpGcsBucketRequest;
import bio.terra.workspace.model.DeleteControlledGcpGcsBucketResult;
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
import bio.terra.workspace.model.GcpDataprocClusterLifecycleConfig;
import bio.terra.workspace.model.GcpDataprocClusterResource;
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
import bio.terra.workspace.model.JobControl;
import bio.terra.workspace.model.UpdateBigQueryDataTableReferenceRequestBody;
import bio.terra.workspace.model.UpdateBigQueryDatasetReferenceRequestBody;
import bio.terra.workspace.model.UpdateControlledGcpAiNotebookInstanceRequestBody;
import bio.terra.workspace.model.UpdateControlledGcpBigQueryDatasetRequestBody;
import bio.terra.workspace.model.UpdateControlledGcpDataprocClusterRequestBody;
import bio.terra.workspace.model.UpdateControlledGcpGcsBucketRequestBody;
import bio.terra.workspace.model.UpdateGcsBucketObjectReferenceRequestBody;
import bio.terra.workspace.model.UpdateGcsBucketReferenceRequestBody;
import com.google.auth.oauth2.AccessToken;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for calling Workspace Manager's GCP endpoints. */
public class WorkspaceManagerServiceGcp extends WorkspaceManagerService {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerServiceGcp.class);

  /**
   * Constructor for class that talks to WSM. If the access token is null, only unauthenticated
   * endpoints can be called.
   */
  private WorkspaceManagerServiceGcp(@Nullable AccessToken accessToken, Server server) {
    super(accessToken, server);
  }

  /**
   * Factory method for class that talks to WSM. No user credentials are used, so only
   * unauthenticated endpoints can be called.
   */
  public static WorkspaceManagerServiceGcp unauthenticated(Server server) {
    return new WorkspaceManagerServiceGcp(null, server);
  }

  /**
   * Factory method for class that talks to WSM. Pulls the current server and user from the context.
   */
  public static WorkspaceManagerServiceGcp fromContext() {
    return new WorkspaceManagerServiceGcp(
        Context.requireUser().getTerraToken(), Context.getServer());
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
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/dataproc-clusters" endpoint to add a
   * GCP dataproc cluster as a controlled resource in the workspace. Clusters can take 5-10 minutes
   * to create, so we do not wait for it to complete.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams resource definition to create
   */
  public GcpDataprocClusterResource createControlledGcpDataprocCluster(
      UUID workspaceId, CreateGcpDataprocClusterParams createParams) {
    // convert the CLI object to a WSM request object
    String jobId = UUID.randomUUID().toString();
    CreateControlledGcpDataprocClusterRequestBody createRequest =
        new CreateControlledGcpDataprocClusterRequestBody()
            .common(createCommonFields(createParams.resourceFields))
            .dataprocCluster(toWsmCreateClusterParams(createParams))
            .jobControl(new JobControl().id(jobId));
    logger.debug("Create controlled GCP Dataproc cluster request {}", createRequest);

    return handleClientExceptions(
        () -> {
          ControlledGcpResourceApi controlledGcpResourceApi =
              new ControlledGcpResourceApi(apiClient);
          // Start the GCP Dataproc cluster creation job.
          HttpUtils.callWithRetries(
              () -> controlledGcpResourceApi.createDataprocCluster(createRequest, workspaceId),
              WorkspaceManagerService::isRetryable);

          // Poll the result endpoint until the job is no longer RUNNING.
          CreatedControlledGcpDataprocClusterResult createResult =
              HttpUtils.pollWithRetries(
                  () -> controlledGcpResourceApi.getCreateDataprocClusterResult(workspaceId, jobId),
                  (result) -> isDone(result.getJobReport()),
                  WorkspaceManagerService::isRetryable,
                  // Creating a GCP Dataproc cluster should take less than ~15 minutes.
                  90,
                  Duration.ofSeconds(10));
          logger.debug("Create controlled GCP Dataproc cluster result {}", createResult);
          throwIfJobNotCompleted(createResult.getJobReport(), createResult.getErrorReport());
          return createResult.getDataprocCluster();
        },
        "Error creating controlled GCP Dataproc cluster in the workspace.");
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
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/dataproc-clusters/{resourceId}"
   * endpoint to update a GCP Dataproc cluster controlled resource in the workspace.
   *
   * @param workspaceId the workspace where the resource exists
   * @param resourceId the resource id
   * @param updateParams resource properties to update
   */
  public void updateControlledDataprocCluster(
      UUID workspaceId, UUID resourceId, UpdateControlledGcpDataprocClusterParams updateParams) {

    ControlledGcpResourceApi gcpResourceApi = new ControlledGcpResourceApi(apiClient);
    String updateErrMsg = "Error updating controlled GCP Dataproc cluster in the workspace";

    // Update WSM metadata fields and primary, secondary worker counts, and graceful decommission
    // timeout.
    UpdateControlledGcpDataprocClusterRequestBody updateRequest =
        new UpdateControlledGcpDataprocClusterRequestBody()
            .name(updateParams.resourceFields.name)
            .description(updateParams.resourceFields.description);

    updateRequest.updateParameters(
        new ControlledDataprocClusterUpdateParameters()
            .numPrimaryWorkers(updateParams.numWorkers)
            .numSecondaryWorkers(updateParams.numSecondaryWorkers)
            .gracefulDecommissionTimeout(updateParams.gracefulDecommissionTimeout));
    callWithRetries(
        () -> gcpResourceApi.updateDataprocCluster(updateRequest, workspaceId, resourceId),
        updateErrMsg);

    // Update autoscaling policy independently. Dataproc api does not allow autoscaling policy and
    // other attributes together.
    if (updateParams.autoscalingPolicyUri != null) {
      UpdateControlledGcpDataprocClusterRequestBody autoscalingPolicyUpdateRequest =
          new UpdateControlledGcpDataprocClusterRequestBody()
              .updateParameters(
                  new ControlledDataprocClusterUpdateParameters()
                      .autoscalingPolicy(updateParams.autoscalingPolicyUri));
      callWithRetries(
          () ->
              gcpResourceApi.updateDataprocCluster(
                  autoscalingPolicyUpdateRequest, workspaceId, resourceId),
          updateErrMsg);
    }

    // Update scheduled deletion independently. Dataproc api does not allow autoscaling policy and
    // other attributes together.
    if (updateParams.idleDeleteTtl != null) {
      UpdateControlledGcpDataprocClusterRequestBody autoscalingPolicyUpdateRequest =
          new UpdateControlledGcpDataprocClusterRequestBody()
              .updateParameters(
                  new ControlledDataprocClusterUpdateParameters()
                      .lifecycleConfig(
                          new GcpDataprocClusterLifecycleConfig()
                              .idleDeleteTtl(updateParams.idleDeleteTtl)));
      callWithRetries(
          () ->
              gcpResourceApi.updateDataprocCluster(
                  autoscalingPolicyUpdateRequest, workspaceId, resourceId),
          updateErrMsg);
    }
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
    DeleteControlledGcpAiNotebookInstanceRequest deleteRequest =
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
          return true;
        },
        "Error deleting controlled GCP Notebook instance in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/gcp/dataproc-clusters/{resourceId}"
   * endpoint to delete a GCP Dataproc cluster as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   * @throws SystemException if the job to delete the GCP Dataproc cluster fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public void deleteControlledGcpDataprocCluster(UUID workspaceId, UUID resourceId) {
    ControlledGcpResourceApi controlledGcpResourceApi = new ControlledGcpResourceApi(apiClient);
    String asyncJobId = UUID.randomUUID().toString();
    DeleteControlledGcpDataprocClusterRequest deleteRequest =
        new DeleteControlledGcpDataprocClusterRequest().jobControl(new JobControl().id(asyncJobId));
    handleClientExceptions(
        () -> {
          // make the initial delete request
          HttpUtils.callWithRetries(
              () ->
                  controlledGcpResourceApi.deleteDataprocCluster(
                      deleteRequest, workspaceId, resourceId),
              WorkspaceManagerService::isRetryable);

          // poll the result endpoint until the job is no longer RUNNING
          DeleteControlledGcpDataprocClusterResult deleteResult =
              HttpUtils.pollWithRetries(
                  () ->
                      controlledGcpResourceApi.getDeleteDataprocClusterResult(
                          workspaceId, asyncJobId),
                  (result) -> isDone(result.getJobReport()),
                  WorkspaceManagerService::isRetryable);
          logger.debug("delete controlled GCP Dataproc cluster result: {}", deleteResult);

          throwIfJobNotCompleted(deleteResult.getJobReport(), deleteResult.getErrorReport());
          return true;
        },
        "Error deleting controlled GCP Dataproc cluster in the workspace.");
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
                  WorkspaceManagerService::isRetryable,
                  /*maxCalls=*/ 12,
                  /*sleepDuration=*/ Duration.ofSeconds(5));
          logger.debug("delete controlled gcs bucket result: {}", deleteResult);

          throwIfJobNotCompleted(deleteResult.getJobReport(), deleteResult.getErrorReport());
          return true;
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
}
