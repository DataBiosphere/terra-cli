package bio.terra.cli.cloud.gcp;

import bio.terra.cli.exception.SystemException;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.cloudres.google.api.services.common.OperationCow;
import bio.terra.cloudres.google.api.services.common.OperationUtils;
import bio.terra.cloudres.google.dataproc.ClusterName;
import bio.terra.cloudres.google.dataproc.DataprocCow;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.services.dataproc.model.Cluster;
import com.google.api.services.dataproc.model.Operation;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.time.Duration;

public class GoogleDataproc {
  private final DataprocCow dataproc;

  public GoogleDataproc(GoogleCredentials credentials) {
    dataproc = CrlUtils.createDataprocCow(credentials);
  }

  public Cluster get(ClusterName clusterName) {
    try {
      return dataproc.clusters().get(clusterName).execute();
    } catch (IOException e) {
      throw new SystemException("Error getting dataproc cluster", e);
    }
  }

  public void start(ClusterName clusterName) {
    try {
      Operation startOperation = dataproc.clusters().start(clusterName).execute();
      pollForSuccess(startOperation, "Error starting dataproc cluster: ");
    } catch (InterruptedException | IOException e) {
      checkFor409BadState(e);
      throw new SystemException("Error starting dataproc cluster", e);
    }
  }

  public void stop(ClusterName clusterName) {
    try {
      Operation stopOperation = dataproc.clusters().stop(clusterName).execute();
      pollForSuccess(stopOperation, "Error stopping dataproc cluster: ");
    } catch (InterruptedException | IOException e) {
      checkFor409BadState(e);
      throw new SystemException("Error stopping dataproc cluster", e);
    }
  }

  private void pollForSuccess(Operation operation, String errorMessage)
      throws InterruptedException, IOException {
    OperationCow<Operation> operationCow = dataproc.regionOperations().operationCow(operation);
    operationCow =
        OperationUtils.pollUntilComplete(
            operationCow, Duration.ofSeconds(5), Duration.ofMinutes(3));
    if (operationCow.getOperation().getError() != null) {
      throw new SystemException(errorMessage + operationCow.getOperation().getError().getMessage());
    }
  }

  /**
   * If the exception is a 409 from GCP with "unable to queue the operation" message, then wrap in a
   * UserActionableException and tell the user to try waiting a few minutes before trying again.
   */
  private void checkFor409BadState(Exception ex) {
    if (ex instanceof GoogleJsonResponseException googleJsonEx) {
      int httpCode = googleJsonEx.getStatusCode();
      String message = googleJsonEx.getDetails().getMessage();
      if (httpCode == HttpStatusCodes.STATUS_CODE_CONFLICT
          && message.contains("unable to queue the operation")) {
        throw new UserActionableException(
            "Error changing notebook state: The cluster is not in the right state to start/stop. Wait a few minutes and try again. (409: unable to queue the operation)",
            googleJsonEx);
      }
    }
  }
}
