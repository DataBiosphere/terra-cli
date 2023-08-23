package bio.terra.cli.service.utils;

import bio.terra.cli.exception.SystemException;
import bio.terra.cloudres.common.ClientConfig;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import bio.terra.cloudres.google.cloudresourcemanager.CloudResourceManagerCow;
import bio.terra.cloudres.google.dataproc.DataprocCow;
import bio.terra.cloudres.google.notebooks.AIPlatformNotebooksCow;
import bio.terra.cloudres.google.storage.StorageCow;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.function.Predicate;
import org.apache.http.HttpStatus;

/** Utilities for working with the Terra Cloud Resource Library. */
public class CrlUtils {

  // For GCP permissions propagation, retry for up to 30 minutes.
  public static final int GCP_RETRY_COUNT = 30;
  public static final Duration GCP_RETRY_SLEEP_DURATION = Duration.ofSeconds(60);

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

  /** This method is used for testing only. */
  public static DataprocCow createDataprocCow(GoogleCredentials googleCredentials) {
    try {
      return DataprocCow.create(clientConfig, googleCredentials);
    } catch (GeneralSecurityException | IOException e) {
      throw new SystemException("Error creating dataproc client.", e);
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

  public static boolean isGcpPermissionsError(Exception e) {
    return ((e instanceof GoogleJsonResponseException)
            && (((GoogleJsonResponseException) e).getStatusCode() == HttpStatus.SC_FORBIDDEN))
        || ((e.getCause() instanceof GoogleJsonResponseException)
            && (((GoogleJsonResponseException) e.getCause()).getStatusCode()
                == HttpStatus.SC_FORBIDDEN));
  }

  public static <T, E extends Exception> T callGcpWithPermissionExceptionRetries(
      HttpUtils.SupplierWithCheckedException<T, E> makeRequest) throws E, InterruptedException {
    return HttpUtils.callWithRetries(
        makeRequest,
        CrlUtils::isGcpPermissionsError,
        CrlUtils.GCP_RETRY_COUNT,
        CrlUtils.GCP_RETRY_SLEEP_DURATION);
  }

  public static <T, E extends Exception> T callGcpWithPermissionExceptionRetries(
      HttpUtils.SupplierWithCheckedException<T, E> makeRequest, Predicate<T> isDone)
      throws E, InterruptedException {
    return HttpUtils.pollWithRetries(
        makeRequest,
        isDone,
        CrlUtils::isGcpPermissionsError,
        CrlUtils.GCP_RETRY_COUNT,
        CrlUtils.GCP_RETRY_SLEEP_DURATION);
  }
}
