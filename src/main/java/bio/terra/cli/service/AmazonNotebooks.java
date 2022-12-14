package bio.terra.cli.service;

import bio.terra.cli.businessobject.SageMakerNotebooksCow;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.workspace.model.AwsCredential;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.sagemaker.model.DescribeNotebookInstanceResponse;
import software.amazon.awssdk.services.sagemaker.model.NotebookInstanceStatus;

public class AmazonNotebooks {
  private final SageMakerNotebooksCow notebooks;

  public AmazonNotebooks(AwsCredential credentials, String location) {
    notebooks = CrlUtils.createNotebooksCow(credentials, location);
  }

  public DescribeNotebookInstanceResponse get(String instanceName) {
    try {
      return notebooks.get(instanceName);
    } catch (SdkException e) {
      checkException(e);
      throw new SystemException("Error getting notebook instance", e);
    }
  }

  public void start(String instanceName) {
    try {
      SdkHttpResponse httpResponse = notebooks.start(instanceName);
      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error starting notebook instance, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      checkNotebookStatus(instanceName, NotebookInstanceStatus.IN_SERVICE);
    } catch (SdkException e) {
      checkException(e);
      throw new SystemException("Error starting notebook instance", e);
    }
  }

  public void stop(String instanceName) {
    try {
      SdkHttpResponse httpResponse = notebooks.stop(instanceName);
      if (!httpResponse.isSuccessful()) {
        throw new SystemException(
            "Error stopping notebook instance, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      checkNotebookStatus(instanceName, NotebookInstanceStatus.STOPPED);
    } catch (SdkException e) {
      checkException(e);
      throw new SystemException("Error stopping notebook instance", e);
    }
  }

  private void checkNotebookStatus(String instanceName, NotebookInstanceStatus status) {
    try {
      DescribeNotebookInstanceResponse response = get(instanceName);
      if (!response.sdkHttpResponse().isSuccessful()) {
        throw new SystemException(
            "Error getting notebook instance status, "
                + response
                    .sdkHttpResponse()
                    .statusText()
                    .orElse(String.valueOf(response.sdkHttpResponse().statusCode())));
      } else if (response.notebookInstanceStatus() != status) {
        throw new UserActionableException(
            "Expected notebook instance status is "
                + status
                + " but current status is "
                + response.notebookInstanceStatus());
      }
    } catch (SdkException e) {
      throw new SystemException("Error getting notebook instance", e);
    }
  }

  private void checkException(Exception ex) {
    if (ex instanceof SdkException) {
      SdkException sdkException = (SdkException) ex;
      if (sdkException.getMessage().contains("not authorized to perform")) {
        throw new UserActionableException(
            "User not authorized to perform operation on cloud platform");
      }
    }
  }
}
