package bio.terra.cli.command.shared.options;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.AwsSageMakerNotebook;
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
  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description = "The Google Cloud location of the instance (if using --instance-id).")
  public String location;

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
          .projectId(workspace.getRequiredGoogleProjectId())
          .location(gcpNotebook.getLocation())
          .instanceId(gcpNotebook.getInstanceId())
          .build();
    } else {
      throw new UserActionableException(
          "Only able to use notebook commands on AI Notebook resources, but specified resource is "
              + resource.getResourceType());
    }
  }

  public AwsSageMakerNotebook toAwsNotebookResource() {
    Workspace workspace = Context.requireWorkspace();
    Resource resource =
        (argGroup.resourceName != null)
            ? workspace.getResource(argGroup.resourceName)
            : workspace.getResource(argGroup.instanceId);

    if (resource.getResourceType().equals(Resource.Type.AWS_SAGEMAKER_NOTEBOOK)) {
      return (AwsSageMakerNotebook) resource;
    } else {
      throw new UserActionableException(
          "Only able to use notebook commands on AWS SageMaker Notebook resources, but specified resource is "
              + resource.getResourceType());
    }
  }

  static class ArgGroup {
    @CommandLine.Option(
        names = "--name",
        description =
            "Name of the resource, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
    public String resourceName;

    @CommandLine.Option(names = "--instance-id", description = "The id of the notebook instance.")
    public String instanceId;
  }
}
