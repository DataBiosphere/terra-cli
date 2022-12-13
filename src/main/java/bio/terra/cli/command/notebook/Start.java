package bio.terra.cli.command.notebook;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.AwsNotebook;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.NotebookInstance;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.service.GoogleNotebooks;
import bio.terra.cli.service.WorkspaceManagerService;
import bio.terra.cloudres.google.notebooks.InstanceName;
import bio.terra.workspace.model.AwsCredential;
import bio.terra.workspace.model.CloudPlatform;
import com.google.api.client.util.Strings;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra notebook start" command. */
@CommandLine.Command(
    name = "start",
    description = "Start a stopped Notebook instance within your workspace.",
    showDefaultValues = true)
public class Start extends BaseCommand {
  @CommandLine.Mixin NotebookInstance instanceOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace workspace = Context.requireWorkspace();

    if (workspace.getCloudPlatform() == CloudPlatform.GCP) {
      InstanceName instanceName = instanceOption.toGcpNotebookInstanceName();
      GoogleNotebooks notebooks = new GoogleNotebooks(Context.requireUser().getPetSACredentials());
      notebooks.start(instanceName);

    } else if (workspace.getCloudPlatform() == CloudPlatform.AWS) {
      AwsNotebook awsNotebook = instanceOption.toAwsNotebookResource();
      AwsCredential awsCredential =
          WorkspaceManagerService.fromContext()
              .getAwsSageMakerNotebookCredential(workspace.getUuid(), awsNotebook.getId());

      OUT.println(
          String.format(
              "TEST log: (start) Fetched awsCredential with nonEmpty fields - keyId: %b, accessKey: %b, sessionToken: %b",
              !Strings.isNullOrEmpty(awsCredential.getAccessKeyId()),
              !Strings.isNullOrEmpty(awsCredential.getSecretAccessKey()),
              !Strings.isNullOrEmpty(awsCredential.getSessionToken())));
      /*
      AwsNotebookInstanceName instanceName =
          instanceOption.toAwsNotebookInstanceName(Context.requireWorkspace(), awsNotebook);
      AmazonNotebooks notebooks =
          new AmazonNotebooks(awsCredential);
      notebooks.start(instanceName);
       */

    } else {
      throw new UserActionableException(
          "Notebooks not supported on workspace cloud platform " + workspace.getCloudPlatform());
    }

    OUT.println("Notebook instance starting. It may take a few minutes before it is available");
  }
}
