package bio.terra.cli.service;

import bio.terra.cli.businessobject.SageMakerNotebooksCow;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.workspace.model.AwsCredential;

public class AmazonNotebooks { // TODO(TERRA-197)
  private final SageMakerNotebooksCow notebooks;

  public AmazonNotebooks(AwsCredential credentials) {
    notebooks = CrlUtils.createNotebooksCow(credentials);
  }
  /*
   public AwsNotebookInstanceName get(AwsNotebookInstanceName instanceName) {
     try {
       return notebooks.instances().get(String.valueOf(instanceName)).execute();
     } catch (IOException e) {
       throw new SystemException("Error getting notebook instance", e);
     }
   }

   public void start(AwsNotebookInstanceName instanceName) {
     try {
       Operation startOperation =
           notebooks.instances().start(String.valueOf(instanceName)).execute();
       pollForSuccess(startOperation, "Error starting notebook instance: ");
     } catch (InterruptedException | IOException e) {
       checkFor409BadState(e);
       throw new SystemException("Error starting notebook instance", e);
     }
   }

   public void stop(AwsNotebookInstanceName instanceName) {
     try {
       Operation stopOperation = notebooks.instances().stop(String.valueOf(instanceName)).execute();
       pollForSuccess(stopOperation, "Error stopping notebook instance: ");
     } catch (InterruptedException | IOException e) {
       checkFor409BadState(e);
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

   /**
    * If the exception is a 409 from GCP with "unable to queue the operation" message, then wrap in a
    * UserActionableException and tell the user to try waiting a few minutes before trying again.
    * There is an open issue against the java-notebooks library on GitHub for this:
    * https://github.com/googleapis/java-notebooks/issues/201.
    * /
   private void checkFor409BadState(Exception ex) { // TODO-dex
     if (ex instanceof GoogleJsonResponseException) {
       GoogleJsonResponseException googleJsonEx = (GoogleJsonResponseException) ex;
       int httpCode = googleJsonEx.getStatusCode();
       String message = googleJsonEx.getDetails().getMessage();
       if (httpCode == HttpStatusCodes.STATUS_CODE_CONFLICT
           && message.contains("unable to queue the operation")) {
         throw new UserActionableException(
             "Error changing notebook state: The notebook is not in the right state to start/stop. Wait a few minutes and try again. (409: unable to queue the operation)",
             googleJsonEx);
       }
     }
   }
  */
}
