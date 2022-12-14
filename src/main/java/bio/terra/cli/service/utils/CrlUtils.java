package bio.terra.cli.service.utils;

import bio.terra.cli.businessobject.SageMakerNotebooksCow;
import bio.terra.cli.exception.SystemException;
import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import bio.terra.cloudres.google.cloudresourcemanager.CloudResourceManagerCow;
import bio.terra.cloudres.google.notebooks.AIPlatformNotebooksCow;
import bio.terra.cloudres.google.storage.StorageCow;
import bio.terra.workspace.model.AwsCredential;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.security.GeneralSecurityException;

/** Utilities for working with the Terra Cloud Resource Library. */
public class CrlUtils {
  private static final ClientConfig clientConfig =
      ClientConfig.Builder.newBuilder().setClient("terra-cli").build();

  public static ClientConfig getClientConfig() {
    return clientConfig;
  }

  public static AIPlatformNotebooksCow createNotebooksCow(GoogleCredentials googleCredentials) {
    try {
      return AIPlatformNotebooksCow.create(clientConfig, googleCredentials);
    } catch (GeneralSecurityException | IOException e) {
      throw new SystemException("Error creating notebooks client.", e);
    }
  }

  public static CloudResourceManagerCow createCloudResourceManagerCow(
      GoogleCredentials googleCredentials) {
    try {
      return CloudResourceManagerCow.create(clientConfig, googleCredentials);
    } catch (GeneralSecurityException | IOException ex) {
      throw new SystemException("Error creating cloud resource manager client.", ex);
    }
  }

  public static StorageCow createStorageCow(GoogleCredentials googleCredentials) {
    return new StorageCow(
        clientConfig, StorageOptions.newBuilder().setCredentials(googleCredentials).build());
  }

  public static BigQueryCow createBigQueryCow(GoogleCredentials googleCredentials) {
    try {
      return BigQueryCow.create(clientConfig, googleCredentials);
    } catch (GeneralSecurityException | IOException ex) {
      throw new SystemException("Error creating Big Query client.", ex);
    }
  }

  public static SageMakerNotebooksCow createNotebooksCow(AwsCredential awsCredential) {
    try {
      return SageMakerNotebooksCow.create(awsCredential);
    } catch (Exception e) {
      throw new SystemException("Error creating notebooks client.", e);
    }
  }
}
