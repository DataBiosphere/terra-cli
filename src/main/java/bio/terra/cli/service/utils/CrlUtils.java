package bio.terra.cli.service.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import bio.terra.cloudres.google.notebooks.AIPlatformNotebooksCow;
import bio.terra.cloudres.google.storage.StorageCow;
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

  public static StorageCow createStorageCow(GoogleCredentials credentials) {
    StorageOptions storageOptions = StorageOptions.newBuilder().setCredentials(credentials).build();
    return new StorageCow(clientConfig, storageOptions);
  }

  public static BigQueryCow createBigQueryCow(GoogleCredentials credentials) {
    try {
      return BigQueryCow.create(clientConfig, credentials);
    } catch (GeneralSecurityException | IOException e) {
      throw new SystemException("Error creating BigQuery client", e);
    }
  }
}
