package bio.terra.cli.context;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.service.utils.GoogleBigQuery;
import bio.terra.cli.service.utils.GoogleCloudStorage;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;

/** This POJO class represents a Terra workspace cloud resource (controlled or external). */
public class CloudResource {
  /**
   * Delimiter between the project id and dataset id for a bigQueryDataset {@link #cloudId}.
   *
   * <p>The choice is somewhat arbitrary. BigQuery Datatsets do not have true URIs. The '.'
   * delimiter allows the cloudId to be used directly in SQL calls with a bigquery extension.
   */
  private static final char DATASET_CLOUD_ID_DELIMITER = '.';

  // name of the cloud resource. names are unique within a workspace
  public String name;

  // cloud identifier for the resource (e.g. bucket uri, bq dataset id)
  public String cloudId;

  // type of resource (e.g. bucket, bq dataset, vm)
  public Type type;

  // true = this cloud resource maps to a controlled resource within the workspace
  // false = this cloud resource maps to an external resource within the workspace
  public boolean isControlled;

  // default constructor required for Jackson de/serialization
  public CloudResource() {}

  public CloudResource(String name, String cloudId, Type type, boolean isControlled) {
    this.name = name;
    this.cloudId = cloudId;
    this.type = type;
    this.isControlled = isControlled;
  }

  /** Type of cloud resource. */
  public enum Type {
    bucket(true),
    // cloudId format is 'projectId.datasetId'.
    bigQueryDataset(true);

    // true means this cloud resource will be included in the list of data references for a
    // workspace
    public final boolean isDataReference;

    Type(boolean isDataReference) {
      this.isDataReference = isDataReference;
    }
  }

  /**
   * Check whether the user has access to this cloud resource.
   *
   * @param terraUser the user whose credentials we use to do the check
   * @return true if the user has access
   */
  public boolean checkAccessForUser(TerraUser terraUser, WorkspaceContext workspaceContext) {
    return checkAccess(terraUser.userCredentials, workspaceContext);
  }

  public static DatasetId cloudIdToDatasetId(String cloudId) {
    int delimiterIndex = cloudId.indexOf(DATASET_CLOUD_ID_DELIMITER);
    if (delimiterIndex == -1) {
      throw new UserActionableException(
          "Expected the cloudId for a dataset to be of the form 'projectId.datasetId', but was "
              + cloudId);
    }
    return DatasetId.of(
        cloudId.substring(0, delimiterIndex), cloudId.substring(delimiterIndex + 1));
  }

  public static String toCloudId(DatasetId datasetId) {
    return datasetId.getProject() + DATASET_CLOUD_ID_DELIMITER + datasetId.getDataset();
  }

  /**
   * Check whether the user's pet SA has access to this cloud resource.
   *
   * @param terraUser the user whose pet SA credentials we use to do the check
   * @return true if the user's pet SA has access
   */
  public boolean checkAccessForPetSa(TerraUser terraUser, WorkspaceContext workspaceContext) {
    return checkAccess(terraUser.petSACredentials, workspaceContext);
  }

  private boolean checkAccess(GoogleCredentials credentials, WorkspaceContext workspaceContext) {
    switch (type) {
      case bucket:
        return new GoogleCloudStorage(credentials, workspaceContext.getGoogleProject())
            .checkObjectsListAccess(cloudId);
      case bigQueryDataset:
        return new GoogleBigQuery(credentials, workspaceContext.getGoogleProject())
            .checkListTablesAccess(CloudResource.cloudIdToDatasetId(cloudId));
    }
    throw new IllegalArgumentException("Unhandled CloudResource type.");
  }
}
