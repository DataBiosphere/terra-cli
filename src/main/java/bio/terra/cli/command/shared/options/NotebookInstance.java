package bio.terra.cli.command.shared.options;

import bio.terra.cli.businessobject.AwsNotebookInstanceName;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.AwsNotebook;
import bio.terra.cli.businessobject.resource.GcpNotebook;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cloudres.google.notebooks.InstanceName;
import picocli.CommandLine;

/**
 * Command helper class for identifying a notebook by either the workspace resource name or the GCP
 * instance name in `terra notebook` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class NotebookInstance {
  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ArgGroup argGroup;

  public InstanceName toGcpNotebookInstanceName() {
    Workspace workspace = Context.requireWorkspace();
    Resource resource =
        (argGroup.resourceName != null)
            ? workspace.getResource(argGroup.resourceName)
            : workspace.getResource(argGroup.instanceId);

    if (resource.getResourceType().equals(Resource.Type.AI_NOTEBOOK)) {
      GcpNotebook gcpNotebook = (GcpNotebook) resource;
      return InstanceName.builder()
          .projectId(workspace.getGoogleProjectId())
          .location(gcpNotebook.getLocation())
          .instanceId(gcpNotebook.getInstanceId())
          .build();

    } else {
      throw new UserActionableException(
          "Only able to use notebook commands on AI Notebook resources, but specified resource is "
              + resource.getResourceType());
    }
  }

  public AwsNotebook toAwsNotebookResource() {
    Workspace workspace = Context.requireWorkspace();
    Resource resource =
        (argGroup.resourceName != null)
            ? workspace.getResource(argGroup.resourceName)
            : workspace.getResource(argGroup.instanceId);

    if (resource.getResourceType().equals(Resource.Type.AWS_SAGEMAKER_NOTEBOOK)) {
      return (AwsNotebook) resource;

    } else {
      throw new UserActionableException(
          "Only able to use notebook commands on SageMaker Notebook resources, but specified resource is "
              + resource.getResourceType());
    }
  }

  public AwsNotebookInstanceName toAwsNotebookInstanceName(
      Workspace workspace, AwsNotebook awsNotebook) {
    return AwsNotebookInstanceName.builder()
        .awsAccountNumber(workspace.getAwsAccountNumber())
        .landingZoneId(workspace.getLandingZoneId())
        .location(awsNotebook.getLocation())
        .instanceId(awsNotebook.getInstanceId())
        .build();
  }

  static class ArgGroup {
    @CommandLine.Option(
        names = "--name",
        description = "Name of the resource, scoped to the workspace.")
    public String resourceName;

    @CommandLine.Option(names = "--instance-id", description = "The id of the notebook instance.")
    public String instanceId;
  }
}
