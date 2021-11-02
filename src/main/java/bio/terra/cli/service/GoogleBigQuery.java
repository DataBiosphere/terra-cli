package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import com.google.api.services.bigquery.model.Dataset;
import com.google.api.services.bigquery.model.TableList;
import com.google.auth.oauth2.GoogleCredentials;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility methods for talking to Google BigQuery. */
public class GoogleBigQuery {
  private static final Logger logger = LoggerFactory.getLogger(GoogleBigQuery.class);

  private final BigQueryCow bigQuery;

  /**
   * Factory method for class that talks to GCS. Pulls the current user from the context. Uses the
   * pet SA credentials instead of the end user credentials because we need the cloud-platform scope
   * to talk to the cloud directly. The CLI does not request the cloud-platform scope during the
   * user login flow, so we need to use the pet SA credentials instead when that scope is needed.
   */
  public static GoogleBigQuery fromContextForPetSa() {
    return new GoogleBigQuery(Context.requireUser().getPetSACredentials());
  }

  private GoogleBigQuery(GoogleCredentials credentials) {
    bigQuery = CrlUtils.createBigQueryCow(credentials);
  }

  public Optional<Dataset> getDataset(String projectId, String datasetId) {
    try {
      return Optional.of(bigQuery.datasets().get(projectId, datasetId).execute());
    } catch (Exception ex) {
      logger.error("Caught exception looking up dataset.", ex);
      return Optional.empty();
    }
  }

  public long getNumTables(String projectId, String datasetId) {
    try {
      TableList tables = bigQuery.tables().list(projectId, datasetId).execute();
      return tables.getTotalItems();
    } catch (Exception ex) {
      logger.error("Caught exception looking up dataset tables.", ex);
      return 0;
    }
  }
}
