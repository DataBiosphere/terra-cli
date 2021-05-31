package bio.terra.cli.service;

import bio.terra.cli.command.exception.SystemException;
import bio.terra.cloudres.google.api.services.common.OperationCow;
import bio.terra.cloudres.google.api.services.common.OperationUtils;
import bio.terra.cloudres.google.notebooks.AIPlatformNotebooksCow;
import bio.terra.cloudres.google.notebooks.InstanceName;
import com.google.api.services.notebooks.v1.model.Instance;
import com.google.api.services.notebooks.v1.model.Operation;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.time.Duration;

public class GoogleAiNotebooks {
  private final AIPlatformNotebooksCow notebooks;

  public GoogleAiNotebooks(GoogleCredentials credentials) {
    notebooks = CrlUtils.createNotebooksCow(credentials);
  }

  public Instance get(InstanceName instanceName) {
    try {
      return notebooks.instances().get(instanceName).execute();
    } catch (IOException e) {
      throw new SystemException("Error getting notebook instance", e);
    }
  }

  public void start(InstanceName instanceName) {
    try {
      Operation startOperation = notebooks.instances().start(instanceName).execute();
      pollForSuccess(startOperation, "Error starting notebook instance: ");
    } catch (InterruptedException | IOException e) {
      throw new SystemException("Error starting notebook instance", e);
    }
  }

  public void stop(InstanceName instanceName) {
    try {
      Operation stopOperation = notebooks.instances().stop(instanceName).execute();
      pollForSuccess(stopOperation, "Error stopping notebook instance: ");
    } catch (InterruptedException | IOException e) {
      throw new SystemException("Error stopping notebook instance", e);
    }
  }

  private void pollForSuccess(Operation operation, String errorMessage)
      throws InterruptedException, IOException {
    OperationCow<Operation> operationCow = notebooks.operations().operationCow(operation);
    operationCow =
        OperationUtils.pollUntilComplete(
            operationCow, Duration.ofSeconds(5), Duration.ofMinutes(3));
    if (operationCow.getOperation().getError() != null) {
      throw new SystemException(errorMessage + operationCow.getOperation().getError().getMessage());
    }
  }
}
