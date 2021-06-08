package harness.utils;

import static harness.TestExternalResources.getProjectId;
import static harness.TestExternalResources.getSACredentials;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.Acl;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/** Utility methods for creating external BQ datasets for testing workspace references. */
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

  /**
   * Create a dataset in the external project. This is helpful for testing referenced BQ dataset
   * resources. This method uses SA credentials for the external project.
   */
  public static Dataset createDataset() throws IOException {
    String datasetId = randomDatasetId();
    String location = "us-east4";

    Dataset dataset =
        getBQClient().create(DatasetInfo.newBuilder(datasetId).setLocation(location).build());

    System.out.println(
        "Created dataset "
            + dataset.getDatasetId().getDataset()
            + " in "
            + dataset.getLocation()
            + " in project "
            + dataset.getDatasetId().getProject());
    return dataset;
  }

  /**
   * Delete a dataset in the external project. This is helpful for testing referenced BQ dataset
   * resources. This method uses SA credentials for the external project.
   */
  public static void deleteDataset(Dataset dataset) throws IOException {
    getBQClient().delete(dataset.getDatasetId());
  }

  /**
   * Grant a given user reader access to a dataset. This method uses SA credentials for the external
   * project.
   */
  public static void grantReadAccess(Dataset dataset, String email) throws IOException {
    BigQuery bigQuery = getBQClient();
    ArrayList<Acl> acls = new ArrayList<>(dataset.getAcl());
    acls.add(Acl.of(new Acl.User(email), Acl.Role.READER));
    bigQuery.update(dataset.toBuilder().setAcl(acls).build());
  }

  /** Helper method to build the BQ client object with SA credentials for the external project. */
  private static BigQuery getBQClient() throws IOException {
    return getBQClient(getSACredentials());
  }

  /** Helper method to build the BQ client object with the given credentials. */
  private static BigQuery getBQClient(GoogleCredentials credentials) throws IOException {
    return BigQueryOptions.newBuilder()
        .setProjectId(getProjectId())
        .setCredentials(credentials)
        .build()
        .getService();
  }
}
