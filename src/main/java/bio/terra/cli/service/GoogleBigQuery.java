package bio.terra.cli.service;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import com.google.api.services.bigquery.model.TableList;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;
import java.io.IOException;

/** Wrapper service for the CRL BigQuery COW. */
public class GoogleBigQuery {
  private final BigQueryCow bigQueryCow;

  public GoogleBigQuery(GoogleCredentials credentials) {
    bigQueryCow = CrlUtils.createBigQueryCow(credentials);
  }

  /**
   * Return an instance of GoogleBigQuery built from the current user's pet service account
   * credentials.
   *
   * @return GoogleBigQuery service instance
   */
  public static GoogleBigQuery fromContextForPetSa() {
    return new GoogleBigQuery(Context.requireUser().getPetSACredentials());
  }

  /**
   * Retrieve the number of tables in the given dataset
   *
   * @param datasetId - FQ ID of dataset in the project
   * @return - number of tables in dataset
   */
  public int getTableCount(DatasetId datasetId) {
    try {
      TableList list =
          bigQueryCow.tables().list(datasetId.getProject(), datasetId.getDataset()).execute();
      return list.getTotalItems();
    } catch (IOException e) {
      throw new SystemException("Error listing dataset tables.", e);
    }
  }
}
