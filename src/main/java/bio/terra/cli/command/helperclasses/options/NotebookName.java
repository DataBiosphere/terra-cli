package bio.terra.cli.command.helperclasses.options;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.cloudres.google.notebooks.InstanceName;
import bio.terra.workspace.model.GcpAiNotebookInstanceAttributes;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/**
 * Command helper class for identifying a notebook by either the resource name of the GCP instance
 * name in `terra notebooks` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class NotebookName {

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ArgGroup argGroup;

  static class ArgGroup {
    @CommandLine.Option(
        names = "--name",
        description =
            "Name of the resource, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
    public String resourceName;

    @CommandLine.Option(names = "--instance-id", description = "The id of the notebook instance.")
    public String instanceId;
  }

  @CommandLine.Option(
      names = "--location",
      defaultValue = "us-central1-a",
      description = "The Google Cloud location of the instance (if using --instance-id).")
  public String location;

  public InstanceName toInstanceName(
      GlobalContext globalContext, WorkspaceContext workspaceContext) {
    if (argGroup.resourceName != null) {
      ResourceDescription resource =
          new WorkspaceManager(globalContext, workspaceContext).getResource(argGroup.resourceName);
      GcpAiNotebookInstanceAttributes attributes =
          resource.getResourceAttributes().getGcpAiNotebookInstance();
      if (attributes == null) {
        throw new UserActionableException(
            "Only able to use notebook commands on notebook resources, but specified resource is "
                + resource.getMetadata().getResourceType());
      }
      return InstanceName.builder()
          .projectId(attributes.getProjectId())
          .location(attributes.getLocation())
          .instanceId(attributes.getInstanceId())
          .build();
    } else {
      return InstanceName.builder()
          .projectId(workspaceContext.getGoogleProject())
          .location(location)
          .instanceId(argGroup.instanceId)
          .build();
    }
  }
}
