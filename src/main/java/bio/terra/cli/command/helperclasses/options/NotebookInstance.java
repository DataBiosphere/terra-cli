package bio.terra.cli.command.helperclasses.options;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Resource;
import bio.terra.cli.context.Workspace;
import bio.terra.cli.context.resources.AiNotebook;
import bio.terra.cloudres.google.notebooks.InstanceName;
import bio.terra.workspace.model.ResourceType;
import picocli.CommandLine;

/**
 * Command helper class for identifying a notebook by either the workspace resource name or the GCP
 * instance name in `terra notebooks` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class NotebookInstance {

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

  public InstanceName toInstanceName() {
    Workspace workspace = GlobalContext.get().requireCurrentWorkspace();
    if (argGroup.resourceName != null) {
      Resource resource = workspace.getResource(argGroup.resourceName);
      if (!resource.resourceType.equals(ResourceType.AI_NOTEBOOK)) {
        throw new UserActionableException(
            "Only able to use notebook commands on notebook resources, but specified resource is "
                + resource.resourceType);
      }
      AiNotebook aiNotebook = (AiNotebook) resource;
      return InstanceName.builder()
          .projectId(aiNotebook.projectId)
          .location(aiNotebook.location)
          .instanceId(aiNotebook.instanceId)
          .build();
    } else {
      return InstanceName.builder()
          .projectId(workspace.googleProjectId)
          .location(location)
          .instanceId(argGroup.instanceId)
          .build();
    }
  }
}
