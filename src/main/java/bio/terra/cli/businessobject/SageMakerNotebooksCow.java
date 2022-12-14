package bio.terra.cli.businessobject;

import bio.terra.workspace.model.AwsCredential;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;
import software.amazon.awssdk.services.sagemaker.SageMakerClientBuilder;
import software.amazon.awssdk.services.sagemaker.model.DescribeNotebookInstanceRequest;
import software.amazon.awssdk.services.sagemaker.model.DescribeNotebookInstanceResponse;
import software.amazon.awssdk.services.sagemaker.model.StartNotebookInstanceRequest;
import software.amazon.awssdk.services.sagemaker.model.StopNotebookInstanceRequest;

// TODO-dex move to CRL

/** A Cloud Object Wrapper(COW) for AWS SageMakerClient Library: {@link SageMakerClient} */
public class SageMakerNotebooksCow {
  private final SageMakerClient notebooks;

  public SageMakerNotebooksCow(SageMakerClientBuilder notebooksBuilder) {
    notebooks = notebooksBuilder.build();
  }

  /** Create a {@link SageMakerNotebooksCow} with some default configurations for convenience. */
  public static SageMakerNotebooksCow create(AwsCredential awsCredential) {
    return new SageMakerNotebooksCow(
        SageMakerClient.builder()
            .region(Region.of("region-dex"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(
                        awsCredential.getAccessKeyId(),
                        awsCredential.getSecretAccessKey(),
                        awsCredential.getSessionToken()))));
  }

  public SageMakerClient instances() {
    return notebooks;
  }

  public DescribeNotebookInstanceResponse get(String instanceName) {
    return notebooks.describeNotebookInstance(
        DescribeNotebookInstanceRequest.builder().notebookInstanceName(instanceName).build());
  }

  public SdkHttpResponse start(String instanceName) {
    return notebooks
        .startNotebookInstance(
            StartNotebookInstanceRequest.builder().notebookInstanceName(instanceName).build())
        .sdkHttpResponse();
  }

  public SdkHttpResponse stop(String instanceName) {
    return notebooks
        .stopNotebookInstance(
            StopNotebookInstanceRequest.builder().notebookInstanceName(instanceName).build())
        .sdkHttpResponse();
  }
}
