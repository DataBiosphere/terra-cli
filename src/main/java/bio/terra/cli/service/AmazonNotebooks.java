package bio.terra.cli.service;

import bio.terra.cli.businessobject.SageMakerNotebooksCow;
import bio.terra.cli.service.utils.CrlUtils;
import bio.terra.workspace.model.AwsCredential;

public class AmazonNotebooks {
  private final SageMakerNotebooksCow notebooks;

  public AmazonNotebooks(AwsCredential credentials, String location) {
    notebooks = CrlUtils.createNotebooksCow(credentials, location);
  }

  public void start(String instanceName) {
    notebooks.start(instanceName);
  }

  public void stop(String instanceName) {
    notebooks.stop(instanceName);
  }
}
