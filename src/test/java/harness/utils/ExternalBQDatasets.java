package harness.utils;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.Acl;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetInfo;
import harness.TestExternalResources;
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
   * Create a dataset in the external project. This is helpful for testing referenced BQ dataset
   * resources. This method uses SA credentials to set IAM policy on a bucket in an external (to
   * WSM) project.
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
   * Grant a given user reader access to a dataset. This method uses SA credentials to set IAM
   * policy on a bucket in an external (to WSM) project.
   */
  public static void grantReadAccess(Dataset dataset, String email) throws IOException {
    BigQuery bigQuery = getBQClient();
    ArrayList<Acl> acls = new ArrayList<>(dataset.getAcl());
    acls.add(Acl.of(new Acl.User(email), Acl.Role.READER));
    bigQuery.update(dataset.toBuilder().setAcl(acls).build());
  }

  /** Helper method to build the BQ client object with SA credentials. */
  public static BigQuery getBQClient() throws IOException {
    return getBQClient(TestExternalResources.getSACredentials());
  }

  /** Helper method to build the BQ client object with the given credentials. */
  public static BigQuery getBQClient(GoogleCredentials credentials) throws IOException {
    return BigQueryOptions.newBuilder()
        .setProjectId(TestExternalResources.getProjectId())
        .setCredentials(credentials)
        .build()
        .getService();
  }
}
