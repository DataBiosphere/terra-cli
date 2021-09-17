package bio.terra.cli.service;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.bigquery.BigQueryCow;
import com.google.api.services.bigquery.model.TableList;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.DatasetId;
import java.io.IOException;

public class GoogleBqDatasets {
  private final BigQueryCow bigQueryCow;

  public GoogleBqDatasets(GoogleCredentials credentials) {
    bigQueryCow = CrlUtils.createBigQueryCow(credentials);
  }

  public int getTableCount(DatasetId datasetid) {
    try {
      TableList list =
          bigQueryCow.tables().list(datasetid.getProject(), datasetid.getDataset()).execute();
      return list.getTotalItems();
    } catch (IOException e) {
      throw new SystemException("Error listing dataset tables.", e);
    }
  }
}
