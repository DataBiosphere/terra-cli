package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.businessobject.resource.AwsSageMakerNotebook;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.input.CreateAwsS3StorageFolderParams;
import bio.terra.cli.serialization.userfacing.input.CreateAwsSageMakerNotebookParams;
import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.workspace.api.ControlledAwsResourceApi;
import bio.terra.workspace.model.AwsCredential;
import bio.terra.workspace.model.AwsCredentialAccessScope;
import bio.terra.workspace.model.AwsS3StorageFolderCreationParameters;
import bio.terra.workspace.model.AwsS3StorageFolderResource;
import bio.terra.workspace.model.AwsSagemakerNotebookCreationParameters;
import bio.terra.workspace.model.AwsSagemakerNotebookResource;
import bio.terra.workspace.model.CreateControlledAwsS3StorageFolderRequestBody;
import bio.terra.workspace.model.CreateControlledAwsSagemakerNotebookRequestBody;
import bio.terra.workspace.model.CreateControlledAwsSagemakerNotebookResult;
import bio.terra.workspace.model.DeleteControlledAwsResourceRequestBody;
import bio.terra.workspace.model.DeleteControlledAwsResourceResult;
import bio.terra.workspace.model.JobControl;
import com.google.auth.oauth2.AccessToken;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;
import software.amazon.awssdk.services.sagemaker.model.DescribeNotebookInstanceRequest;
import software.amazon.awssdk.services.sagemaker.model.DescribeNotebookInstanceResponse;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;
import software.amazon.awssdk.services.sagemaker.model.StartNotebookInstanceRequest;
import software.amazon.awssdk.services.sagemaker.model.StopNotebookInstanceRequest;
import software.amazon.awssdk.services.sagemaker.waiters.SageMakerWaiter;

/** Utility methods for calling Workspace Manager's AWS endpoints. */
public class WorkspaceManagerServiceAws extends WorkspaceManagerService {
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceManagerServiceAws.class);
  private static final int CREDENTIAL_EXPIRATION_SECONDS_DEFAULT = 900;
  private static final Duration SAGEMAKER_CLIENT_WAITER_TIMEOUT = Duration.ofSeconds(1800);
  public static final Set<NotebookInstanceStatus> notebookStatusSetCanStart =
      Set.of(NotebookInstanceStatus.STOPPED, NotebookInstanceStatus.FAILED);
  public static final Set<NotebookInstanceStatus> notebookStatusSetCanStop =
      Set.of(
          NotebookInstanceStatus.PENDING,
          NotebookInstanceStatus.IN_SERVICE,
          NotebookInstanceStatus.UPDATING);

  /**
   * Constructor for class that talks to WSM. If the access token is null, only unauthenticated
   * endpoints can be called.
   */
  private WorkspaceManagerServiceAws(@Nullable AccessToken accessToken, Server server) {
    super(accessToken, server);
  }

  /**
   * Factory method for class that talks to WSM. No user credentials are used, so only
   * unauthenticated endpoints can be called.
   */
  public static WorkspaceManagerServiceAws unauthenticated(Server server) {
    return new WorkspaceManagerServiceAws(null, server);
  }

  /**
   * Factory method for class that talks to WSM. Pulls the current server and user from the context.
   */
  public static WorkspaceManagerServiceAws fromContext() {
    return new WorkspaceManagerServiceAws(
        Context.requireUser().getTerraToken(), Context.getServer());
  }

  private void deleteControlledAwsResource(UUID workspaceId, UUID resourceId) {
    String asyncJobId = UUID.randomUUID().toString();
    DeleteControlledAwsResourceRequestBody deleteRequest =
        new DeleteControlledAwsResourceRequestBody().jobControl(new JobControl().id(asyncJobId));

    handleClientExceptions(
        () -> {
          ControlledAwsResourceApi controlledAwsResourceApi =
              new ControlledAwsResourceApi(apiClient);
          // make the initial delete request
          HttpUtils.callWithRetries(
              () ->
                  controlledAwsResourceApi.deleteAwsS3StorageFolder(
                      deleteRequest, workspaceId, resourceId),
              WorkspaceManagerService::isRetryable);

          // poll the result endpoint until the job is no longer RUNNING
          DeleteControlledAwsResourceResult deleteResult =
              HttpUtils.pollWithRetries(
                  () ->
                      controlledAwsResourceApi.getDeleteAwsS3StorageFolderResult(
                          workspaceId, asyncJobId),
                  (result) -> isDone(result.getJobReport()),
                  WorkspaceManagerService::isRetryable,
                  60,
                  Duration.ofSeconds(10));
          logger.debug("delete controlled AWS resource result: {}", deleteResult);

          throwIfJobNotCompleted(deleteResult.getJobReport(), deleteResult.getErrorReport());
          return true;
        },
        String.format(
            "Error deleting controlled AWS resource %s in the workspace %s.",
            workspaceId, resourceId));
  }

  public AwsCredential getAwsResourceCredential(
      UUID workspaceId, UUID resourceId, AwsCredentialAccessScope accessScope, Integer duration) {

    return callWithRetries(
        () ->
            new ControlledAwsResourceApi(apiClient)
                .getAwsS3StorageFolderCredential(workspaceId, resourceId, accessScope, duration),
        String.format(
            "Error getting AWS resource %s credential in the workspace %s.",
            workspaceId, resourceId));
  }

  // S3 Storage Folder

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/aws/storageFolder" endpoint to add a AWS
   * storage folder as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams creation parameters
   * @return the AWS S3 Storage Folder resource object
   */
  public AwsS3StorageFolderResource createControlledAwsS3StorageFolder(
      UUID workspaceId, CreateAwsS3StorageFolderParams createParams) {
    // convert the CLI object to a WSM request object
    CreateControlledAwsS3StorageFolderRequestBody createRequest =
        new CreateControlledAwsS3StorageFolderRequestBody()
            .common(createCommonFields(createParams.resourceFields))
            .awsS3StorageFolder(
                new AwsS3StorageFolderCreationParameters()
                    .folderName(createParams.folderName)
                    .region(createParams.region));
    return callWithRetries(
        () ->
            new ControlledAwsResourceApi(apiClient)
                .createAwsS3StorageFolder(createRequest, workspaceId)
                .getAwsS3StorageFolder(),
        "Error creating controlled AWS S3 Storage Folder in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/aws/storageFolder/{resourceId}" endpoint
   * to delete a AWS S3 Storage Folder as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   * @throws SystemException if the job to delete the storage folder fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public void deleteControlledAwsS3StorageFolder(UUID workspaceId, UUID resourceId) {
    deleteControlledAwsResource(workspaceId, resourceId);
  }

  /**
   * Call the Workspace Manager
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/aws/storageFolder/{resourceId}/credential"
   * endpoint to get AWS credentials to access the controlled storage folder
   *
   * @param workspaceId the workspace that contains the resource
   * @param resourceId the resource id
   * @param accessScope the access scope (READ_ONLY, WRITE_READ)
   * @param duration the duration for credential in seconds
   * @return AWS Bucket access credentials
   */
  public AwsCredential getAwsS3StorageFolderCredential(
      UUID workspaceId, UUID resourceId, AwsCredentialAccessScope accessScope, Integer duration) {
    return getAwsResourceCredential(workspaceId, resourceId, accessScope, duration);
  }

  // SageMaker Notebook

  /**
   * This method converts this CLI-defined POJO class into the WSM client library-defined request
   * object.
   *
   * @return AWS SageMaker Notebook attributes in the format expected by the WSM client library
   */
  private static AwsSagemakerNotebookCreationParameters fromCLIObject(
      CreateAwsSageMakerNotebookParams createParams) {
    return new AwsSagemakerNotebookCreationParameters()
        .instanceName(createParams.instanceName)
        .instanceType(createParams.instanceType)
        .region(createParams.region);
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/aws/notebook" endpoint to add a AWS
   * notebook as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to add the resource to
   * @param createParams creation parameters
   * @return the AWS SageMaker Notebook resource object
   */
  public AwsSagemakerNotebookResource createControlledAwsSageMakerNotebook(
      UUID workspaceId, CreateAwsSageMakerNotebookParams createParams) {
    String asyncJobId = UUID.randomUUID().toString();
    CreateControlledAwsSagemakerNotebookRequestBody createRequest =
        new CreateControlledAwsSagemakerNotebookRequestBody()
            .common(createCommonFields(createParams.resourceFields))
            .awsSagemakerNotebook(fromCLIObject(createParams))
            .jobControl(new JobControl().id(asyncJobId));

    return handleClientExceptions(
        () -> {
          ControlledAwsResourceApi controlledAwsResourceApi =
              new ControlledAwsResourceApi(apiClient);
          // make the initial create request
          HttpUtils.callWithRetries(
              () -> controlledAwsResourceApi.createAwsSagemakerNotebook(createRequest, workspaceId),
              WorkspaceManagerService::isRetryable);

          // poll the result endpoint until the job is no longer RUNNING
          CreateControlledAwsSagemakerNotebookResult createResult =
              HttpUtils.pollWithRetries(
                  () ->
                      controlledAwsResourceApi.getCreateAwsSagemakerNotebookResult(
                          workspaceId, asyncJobId),
                  (result) -> isDone(result.getJobReport()),
                  WorkspaceManagerService::isRetryable,
                  60,
                  Duration.ofSeconds(10));
          logger.debug("create controlled AWS SageMaker Notebook result: {}", createResult);

          throwIfJobNotCompleted(createResult.getJobReport(), createResult.getErrorReport());
          return createResult.getAwsSagemakerNotebook();
        },
        "Error creating controlled AWS SageMaker Notebook in the workspace.");
  }

  /**
   * Call the Workspace Manager POST
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/aws/notebook/{resourceId}" endpoint to
   * delete a AWS SageMaker Notebook as a controlled resource in the workspace.
   *
   * @param workspaceId the workspace to remove the resource from
   * @param resourceId the resource id
   * @throws SystemException if the job to delete the notebook fails
   * @throws UserActionableException if the CLI times out waiting for the job to complete
   */
  public void deleteControlledAwsSageMakerNotebook(UUID workspaceId, UUID resourceId) {
    deleteControlledAwsResource(workspaceId, resourceId);
  }

  public AwsCredential getAwsSageMakerNotebookCredential(UUID workspaceId, UUID resourceId) {
    return getAwsSageMakerNotebookCredential(
        workspaceId,
        resourceId,
        AwsCredentialAccessScope.READ_ONLY,
        CREDENTIAL_EXPIRATION_SECONDS_DEFAULT);
  }

  /**
   * Call the Workspace Manager
   * "/api/workspaces/v1/{workspaceId}/resources/controlled/aws/notebook/{resourceId}/credential"
   * endpoint to get AWS credentials to access the controlled Notebook
   *
   * @param workspaceId the workspace that contains the resource
   * @param resourceId the resource id
   * @param accessScope the access scope (READ_ONLY, WRITE_READ)
   * @param duration the duration for credential in seconds
   * @return AWS Bucket access credentials
   */
  public AwsCredential getAwsSageMakerNotebookCredential(
      UUID workspaceId, UUID resourceId, AwsCredentialAccessScope accessScope, Integer duration) {
    return getAwsResourceCredential(workspaceId, resourceId, accessScope, duration);
    // TODO(TERRA-320) add ProxyUrl here
  }

  // TODO(TERRA-563) move these to CRL

  public static SageMakerClient getSageMakerClient(AwsCredential awsCredential, String region) {
    return SageMakerClient.builder()
        .region(Region.of(region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsSessionCredentials.create(
                    awsCredential.getAccessKeyId(),
                    awsCredential.getSecretAccessKey(),
                    awsCredential.getSessionToken())))
        .build();
  }

  public NotebookInstanceStatus getAwsSageMakerNotebookInstanceStatus(
      AwsSageMakerNotebook awsNotebook, SageMakerClient sageMakerClient) {
    try {
      DescribeNotebookInstanceResponse describeResponse =
          sageMakerClient.describeNotebookInstance(
              DescribeNotebookInstanceRequest.builder()
                  .notebookInstanceName(awsNotebook.getInstanceName())
                  .build());
      SdkHttpResponse httpResponse = describeResponse.sdkHttpResponse();
      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error getting notebook instance, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      return describeResponse.notebookInstanceStatus();

    } catch (SdkException e) {
      checkException(e);
      throw new SystemException("Error getting notebook instance", e);
    }
  }

  public static void waitForSageMakerNotebookStatus(
      AwsSageMakerNotebook awsNotebook,
      NotebookInstanceStatus desiredStatus,
      SageMakerClient sageMakerClient) {
    SageMakerWaiter sageMakerWaiter =
        SageMakerWaiter.builder()
            .client(sageMakerClient)
            .overrideConfiguration(
                WaiterOverrideConfiguration.builder()
                    .waitTimeout(SAGEMAKER_CLIENT_WAITER_TIMEOUT)
                    .build())
            .build();

    DescribeNotebookInstanceRequest describeRequest =
        DescribeNotebookInstanceRequest.builder()
            .notebookInstanceName(awsNotebook.getInstanceName())
            .build();

    try {
      WaiterResponse<DescribeNotebookInstanceResponse> waiterResponse =
          switch (desiredStatus) {
            case IN_SERVICE -> sageMakerWaiter.waitUntilNotebookInstanceInService(describeRequest);
            case STOPPED -> sageMakerWaiter.waitUntilNotebookInstanceStopped(describeRequest);
            default -> throw new UserActionableException(
                "Can only wait for notebook InService or Stopped");
          };

      ResponseOrException<DescribeNotebookInstanceResponse> responseOrException =
          waiterResponse.matched();
      if (responseOrException.exception().isPresent()) {
        throw responseOrException.exception().get();
      }

    } catch (Throwable t) {
      if (t instanceof SdkException e) {
        checkException(e);
      }
      throw new SystemException("Error waiting for desired sagemaker notebook status,", t);
    }
  }

  public void startAwsSageMakerNotebook(UUID workspaceId, AwsSageMakerNotebook awsNotebook) {
    SageMakerClient sageMakerClient =
        getSageMakerClient(
            getAwsSageMakerNotebookCredential(
                workspaceId,
                awsNotebook.getId(),
                AwsCredentialAccessScope.READ_ONLY,
                CREDENTIAL_EXPIRATION_SECONDS_DEFAULT),
            awsNotebook.getRegion());

    try {
      NotebookInstanceStatus currentStatus =
          getAwsSageMakerNotebookInstanceStatus(awsNotebook, sageMakerClient);
      if (currentStatus == NotebookInstanceStatus.IN_SERVICE) {
        return;
      }
      checkNotebookStatus(notebookStatusSetCanStart, currentStatus);

      SdkHttpResponse httpResponse =
          sageMakerClient
              .startNotebookInstance(
                  StartNotebookInstanceRequest.builder()
                      .notebookInstanceName(awsNotebook.getInstanceName())
                      .build())
              .sdkHttpResponse();
      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error starting notebook instance, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      waitForSageMakerNotebookStatus(
          awsNotebook, NotebookInstanceStatus.IN_SERVICE, sageMakerClient);

    } catch (SdkException e) {
      checkException(e);
      throw new SystemException("Error starting notebook instance", e);
    }
  }

  public void stopAwsSageMakerNotebook(UUID workspaceId, AwsSageMakerNotebook awsNotebook) {
    SageMakerClient sageMakerClient =
        getSageMakerClient(
            getAwsSageMakerNotebookCredential(
                workspaceId,
                awsNotebook.getId(),
                AwsCredentialAccessScope.READ_ONLY,
                CREDENTIAL_EXPIRATION_SECONDS_DEFAULT),
            awsNotebook.getRegion());

    try {
      NotebookInstanceStatus currentStatus =
          getAwsSageMakerNotebookInstanceStatus(awsNotebook, sageMakerClient);
      if (notebookStatusSetCanStart.contains(currentStatus)) {
        return;
      }
      checkNotebookStatus(notebookStatusSetCanStop, currentStatus);

      SdkHttpResponse httpResponse =
          sageMakerClient
              .stopNotebookInstance(
                  StopNotebookInstanceRequest.builder()
                      .notebookInstanceName(awsNotebook.getInstanceName())
                      .build())
              .sdkHttpResponse();
      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error stopping notebook instance, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      waitForSageMakerNotebookStatus(awsNotebook, NotebookInstanceStatus.STOPPED, sageMakerClient);

    } catch (SdkException e) {
      checkException(e);
      throw new SystemException("Error stopping notebook instance", e);
    }
  }

  private void checkNotebookStatus(
      Set<NotebookInstanceStatus> expectedStatusSet, NotebookInstanceStatus currentStatus) {
    if (!expectedStatusSet.contains(currentStatus)) {
      throw new UserActionableException(
          "Expected notebook instance status is "
              + expectedStatusSet
              + " but current status is "
              + currentStatus);
    }
  }

  public static void checkException(Exception ex) {
    if (ex instanceof SdkException) {
      String message = ex.getMessage();
      if (message.contains("not authorized to perform")) {
        throw new UserActionableException(
            "Error performing notebook operation, check the instance name / permissions and retry");
      } else if (message.contains("Unable to transition to")) {
        throw new UserActionableException("Unable to perform notebook operation on cloud platform");
      }
    }
  }
}
