package harness.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import harness.TestExternalResources;
import java.io.IOException;
import java.util.UUID;

/** Utility methods for creating external GCS buckets for testing workspace references. */
public class ExternalBQDatasets {

  /** Helper method to generate a random dataset id. */
  public static String randomDatasetId() {
    return UUID.randomUUID().toString().replace('-', '_');
  }

  /**
   * Get a dataset. This is helpful for testing controlled BQ dataset resources. It allows tests to
   * check metadata that is not stored in WSM, only in BQ. This method takes in the credentials to
   * use because tests typically want to check metadata as the test user.
   */
  public static Dataset getDataset(
      String projectId, String datasetId, GoogleCredentials credentials) throws IOException {
    DatasetId datasetRef = DatasetId.of(projectId, datasetId);
    return getBQClient(credentials).getDataset(datasetRef);
  }

  /** Helper method to build the BQ client object with the given credentials. */
  private static BigQuery getBQClient(GoogleCredentials credentials) throws IOException {
    return BigQueryOptions.newBuilder()
        .setProjectId(TestExternalResources.gcpProjectId)
        .setCredentials(credentials)
        .build()
        .getService();
  }
}
