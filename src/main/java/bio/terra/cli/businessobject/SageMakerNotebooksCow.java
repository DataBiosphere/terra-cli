package bio.terra.cli.businessobject;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.workspace.model.AwsCredential;
import java.time.Duration;
import java.util.Set;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;
import software.amazon.awssdk.services.sagemaker.model.DescribeNotebookInstanceRequest;
import software.amazon.awssdk.services.sagemaker.model.DescribeNotebookInstanceResponse;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;
import software.amazon.awssdk.services.sagemaker.model.StartNotebookInstanceRequest;
import software.amazon.awssdk.services.sagemaker.model.StopNotebookInstanceRequest;
import software.amazon.awssdk.services.sagemaker.waiters.SageMakerWaiter;

// TODO(TERRA-225) move to CRL

/** A Cloud Object Wrapper(COW) for AWS SageMakerClient Library: {@link SageMakerClient} */
public class SageMakerNotebooksCow {
  private static final Duration AWS_NOTEBOOK_WAITER_TIMEOUT_DURATION = Duration.ofSeconds(900);
  private final Set<NotebookInstanceStatus> startableStatusSet =
      Set.of(NotebookInstanceStatus.STOPPED, NotebookInstanceStatus.FAILED);
  private final Set<NotebookInstanceStatus> stoppableStatusSet =
      Set.of(NotebookInstanceStatus.IN_SERVICE);
  private final SageMakerClient notebooksClient;
  private final SageMakerWaiter notebooksWaiter;

  public SageMakerNotebooksCow(SageMakerClient notebooksClient, SageMakerWaiter notebooksWaiter) {
    this.notebooksClient = notebooksClient;
    this.notebooksWaiter = notebooksWaiter;
  }

  /** Create a {@link SageMakerNotebooksCow} with some default configurations for convenience. */
  public static SageMakerNotebooksCow create(AwsCredential awsCredential, String location) {
    SageMakerClient notebooksClient =
        SageMakerClient.builder()
            .region(Region.of(location))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(
                        awsCredential.getAccessKeyId(),
                        awsCredential.getSecretAccessKey(),
                        awsCredential.getSessionToken())))
            .build();
    return new SageMakerNotebooksCow(
        notebooksClient,
        SageMakerWaiter.builder()
            .client(notebooksClient)
            .overrideConfiguration(
                WaiterOverrideConfiguration.builder()
                    .waitTimeout(AWS_NOTEBOOK_WAITER_TIMEOUT_DURATION)
                    .build())
            .build());
  }

  private DescribeNotebookInstanceResponse get(String instanceName) {
    return notebooksClient.describeNotebookInstance(
        DescribeNotebookInstanceRequest.builder().notebookInstanceName(instanceName).build());
  }

  public void start(String instanceName) {
    try {
      checkNotebookStatus(instanceName, startableStatusSet);
      SdkHttpResponse httpResponse =
          notebooksClient
              .startNotebookInstance(
                  StartNotebookInstanceRequest.builder().notebookInstanceName(instanceName).build())
              .sdkHttpResponse();

      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error starting notebook instance, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      pollForNotebookStatus(instanceName, NotebookInstanceStatus.IN_SERVICE);

    } catch (SdkException e) {
      checkException(e);
      throw new SystemException("Error starting notebook instance", e);
    }
  }

  public void stop(String instanceName) {
    try {
      checkNotebookStatus(instanceName, stoppableStatusSet);
      SdkHttpResponse httpResponse =
          notebooksClient
              .stopNotebookInstance(
                  StopNotebookInstanceRequest.builder().notebookInstanceName(instanceName).build())
              .sdkHttpResponse();

      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error stopping notebook instance, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      pollForNotebookStatus(instanceName, NotebookInstanceStatus.STOPPED);

    } catch (SdkException e) {
      checkException(e);
      throw new SystemException("Error starting notebook instance", e);
    }
  }

  public void pollForNotebookStatus(String instanceName, NotebookInstanceStatus status) {
    //  try{
    //  var test =notebooksWaiter.waitUntilNotebookInstanceInService();
    // }

    // TODO-Dex
    // if (operationCow.getOperation().getError() != null) {
    // throw new SystemException(errorMessage +
    // operationCow.getOperation().getError().getMessage());
    // }
  }

  public void checkNotebookStatus(String instanceName, Set<NotebookInstanceStatus> statusSet) {
    try {
      DescribeNotebookInstanceResponse response = get(instanceName);
      if (!response.sdkHttpResponse().isSuccessful()) {
        throw new SystemException(
            "Error getting notebook instance status, "
                + response
                    .sdkHttpResponse()
                    .statusText()
                    .orElse(String.valueOf(response.sdkHttpResponse().statusCode())));

      } else if (statusSet.contains(response.notebookInstanceStatus())) {
        throw new UserActionableException(
            "Expected notebook instance status is "
                + statusSet
                + " but current status is "
                + response.notebookInstanceStatus());
      }

    } catch (SdkException e) {
      throw new SystemException("Error checking notebook instance status", e);
    }
  }

  public void checkException(Exception ex) {
    if (ex instanceof SdkException) {
      String message = ex.getMessage();
      if (message.contains("not authorized to perform")) {
        throw new UserActionableException(
            "User not authorized to perform notebook operation on cloud platform");
      } else if (message.contains("Unable to transition to")) {
        throw new UserActionableException("Unable to perform notebook operation on cloud platform");
      }
    }
  }
}
