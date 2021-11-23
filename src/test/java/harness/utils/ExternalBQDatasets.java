package harness.utils;

import bio.terra.cli.service.utils.HttpUtils;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import com.google.api.services.bigquery.model.Dataset;
import com.google.api.services.bigquery.model.DatasetReference;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import harness.CRLJanitor;
import harness.TestExternalResources;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.apache.http.HttpStatus;

/**
 * Utility methods for creating external BQ datasets for testing workspace references. Most methods
 * in the class use the CRL wrapper around the BQ client library to manipulate external datasets.
 * This class also includes a method to build the unwrapped BQ client object, which is what we would
 * expect users to call. Setup/cleanup of external datasets should use the CRL wrapper. Fetching
 * information from the cloud as a user may do should use the unwrapped client object.
 */
public class ExternalBQDatasets {

  /** Helper method to generate a random dataset id. */
  public static String randomDatasetId() {
    return UUID.randomUUID().toString().replace('-', '_');
  }

  /**
   * Create a dataset in the external project. This is helpful for testing referenced BQ dataset
   * resources. This method uses SA credentials that have permissions on the external (to WSM)
   * project.
   */
  public static DatasetReference createDataset() throws IOException {
    String projectId = TestExternalResources.getProjectId();
    String datasetId = randomDatasetId();
    String location = "us-east4";

    DatasetReference datasetReference =
        new DatasetReference().setDatasetId(datasetId).setProjectId(projectId);
    Dataset datasetToCreate =
        new Dataset().setDatasetReference(datasetReference).setLocation(location);
    Dataset dataset = getBQCow().datasets().insert(projectId, datasetToCreate).execute();

    System.out.println(
        "Created dataset "
            + dataset.getDatasetReference().getDatasetId()
            + " in "
            + dataset.getLocation()
            + " in project "
            + dataset.getDatasetReference().getProjectId());
    return dataset.getDatasetReference();
  }

  /**
   * Delete a dataset in the external project. This method uses SA credentials that have permissions
   * on the external (to WSM) project.
   */
  public static void deleteDataset(DatasetReference datasetRef) throws IOException {
    getBQCow()
        .datasets()
        .delete(datasetRef.getProjectId(), datasetRef.getDatasetId())
        .setDeleteContents(true)
        .execute();
  }

  /**
   * Helper enum to distinguish between two different types of IAM members: users and groups. This
   * information could be conveyed using a boolean flag, but it makes the method signature more
   * readable to use an enum instead.
   */
  public enum IamMemberType {
    USER,
    GROUP;
  }

  /**
   * Grant a given user or group READER access to a dataset. This method uses SA credentials that
   * have permissions on the external (to WSM) project.
   */
  public static void grantReadAccess(
      DatasetReference datasetRef, String memberEmail, IamMemberType memberType)
      throws IOException {
    grantAccess(datasetRef, memberEmail, memberType, "READER");
  }

  /**
   * Grant a given user or group WRITER access to a dataset. This method uses SA credentials that
   * have permissions on the external (to WSM) project.
   */
  public static void grantWriteAccess(
      DatasetReference datasetRef, String memberEmail, IamMemberType memberType)
      throws IOException {
    grantAccess(datasetRef, memberEmail, memberType, "WRITER");
  }

  /**
   * Grant a given user or group access to a dataset. This method uses SA credentials that have
   * permissions on the external (to WSM) project.
   */
  private static void grantAccess(
      DatasetReference datasetRef, String memberEmail, IamMemberType memberType, String role)
      throws IOException {
    BigQueryCow bigQuery = getBQCow();
    Dataset datasetToUpdate =
        bigQuery.datasets().get(datasetRef.getProjectId(), datasetRef.getDatasetId()).execute();
    List<Dataset.Access> accessToUpdate = datasetToUpdate.getAccess();

    Dataset.Access newAccess = new Dataset.Access().setRole(role);
    if (memberType.equals(IamMemberType.USER)) {
      newAccess.setUserByEmail(memberEmail);
    } else {
      newAccess.setGroupByEmail(memberEmail);
    }
    accessToUpdate.add(newAccess);
    datasetToUpdate.setAccess(accessToUpdate);

    bigQuery
        .datasets()
        .update(datasetRef.getProjectId(), datasetRef.getDatasetId(), datasetToUpdate)
        .execute();
  }

  /** Utility method to create an arbitrary table in a dataset. */
  public static void createTable(
      GoogleCredentials credentials, String projectId, String datasetId, String tableName)
      throws InterruptedException {
    BigQuery bqClient = getBQClient(credentials);

    // table field definition
    String fieldName = "test_string_field";
    Field field = Field.of(fieldName, LegacySQLTypeName.STRING);

    // table schema definition
    Schema schema = Schema.of(field);
    TableDefinition tableDefinition = StandardTableDefinition.of(schema);

    // table definition
    TableId tableId = TableId.of(projectId, datasetId, tableName);
    TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

    // retry forbidden errors because we often see propagation delays when a user is just granted
    // access
    HttpUtils.callWithRetries(
        () -> {
          bqClient.create(tableInfo);
          System.out.println("Created BQ data table " + tableName + "in data set " + datasetId);
          return null;
        },
        (ex) ->
            (ex instanceof BigQueryException)
                && ((BigQueryException) ex).getCode() == HttpStatus.SC_FORBIDDEN,
        5,
        Duration.ofMinutes(1));
  }

  /**
   * Helper method to build the CRL wrapper around the BQ client object with SA credentials that
   * have permissions on the external (to WSM) project.
   */
  private static BigQueryCow getBQCow() {
    try {
      return BigQueryCow.create(
          CRLJanitor.DEFAULT_CLIENT_CONFIG, TestExternalResources.getSACredentials());
    } catch (GeneralSecurityException | IOException ex) {
      throw new RuntimeException("Error getting Janitor client SA credentials.", ex);
    }
  }

  /**
   * Helper method to build the BQ client object with the given credentials. Note this is not
   * wrapped by CRL and uses the cloud client libraries instead of the api services version because
   * this is what we expect most users to use.
   */
  public static BigQuery getBQClient(GoogleCredentials credentials) {
    return BigQueryOptions.newBuilder()
        .setProjectId(TestExternalResources.getProjectId())
        .setCredentials(credentials)
        .build()
        .getService();
  }

  public static String getDatasetFullPath(String projectId, String datasetId) {
    return projectId + "." + datasetId;
  }

  public static String getDataTablePath(String projectId, String datasetId, String dataTableId) {
    return projectId + "." + datasetId + "." + dataTableId;
  }
}
