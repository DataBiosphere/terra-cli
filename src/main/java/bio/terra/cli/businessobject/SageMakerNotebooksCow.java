package bio.terra.cli.businessobject;

import bio.terra.workspace.model.AwsCredential;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sagemaker.SageMakerAsyncClient;
import software.amazon.awssdk.services.sagemaker.SageMakerAsyncClientBuilder;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;

// TODO-dex move to CRL

/** A Cloud Object Wrapper(COW) for AWS SageMakerClient Library: {@link SageMakerClient} */
public class SageMakerNotebooksCow {
  private final Logger logger = LoggerFactory.getLogger(SageMakerNotebooksCow.class);

  private final SageMakerAsyncClient notebooks;

  public SageMakerNotebooksCow(SageMakerAsyncClientBuilder notebooksBuilder) {
    notebooks = notebooksBuilder.build();
  }

  /** Create a {@link SageMakerNotebooksCow} with some default configurations for convenience. */
  public static SageMakerNotebooksCow create(AwsCredential awsCredential)
      throws GeneralSecurityException, IOException {
    return new SageMakerNotebooksCow(
        SageMakerAsyncClient.builder()
            .region(Region.of("region-dex"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(
                        awsCredential.getAccessKeyId(),
                        awsCredential.getSecretAccessKey(),
                        awsCredential.getSessionToken()))));
  }
}
