package bio.terra.cli.command.shared.options;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.businessobject.resource.AwsNotebook;
import bio.terra.cli.businessobject.resource.GcpNotebook;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cloudres.google.notebooks.InstanceName;
import picocli.CommandLine;

// TODO(TERRA-197)

/**
 * Command helper class for identifying a notebook by either the workspace resource name or the GCP
 * instance name in `terra notebook` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class NotebookInstance {
  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a", // TODO-Dex
      description = "The Google Cloud location of the instance (if using --instance-id).")
  public String location;

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ArgGroup argGroup;

  public InstanceName toInstanceName() {
    Workspace workspace = Context.requireWorkspace();

    if (argGroup.resourceName != null) { // provided resourceName
      Resource resource = workspace.getResource(argGroup.resourceName);
      Resource.Type resourceType = resource.getResourceType();

      if (resourceType.equals(Resource.Type.AI_NOTEBOOK)) {
        GcpNotebook gcpNotebook = (GcpNotebook) resource;
        return InstanceName.builder()
            .projectId(gcpNotebook.getProjectId())
            .location(gcpNotebook.getLocation())
            .instanceId(gcpNotebook.getInstanceId())
            .build();

      } else if (resourceType.equals(Resource.Type.AWS_SAGEMAKER_NOTEBOOK)) {

        AwsNotebook awsNotebook = (AwsNotebook) resource;
        return InstanceName.builder()
            //   .projectId(gcpNotebook.getProjectId())
            .location(awsNotebook.getLocation())
            .instanceId(awsNotebook.getInstanceId())
            .build();

      } else {
        throw new UserActionableException(
            "Only able to use notebook commands on notebook resources, but specified resource is "
                + resource.getResourceType());
      }

    } else { // provided instanceId
      return InstanceName.builder()
          .projectId(workspace.getGoogleProjectId())
          .location(location)
          .instanceId(argGroup.instanceId)
          .build();
    }
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
