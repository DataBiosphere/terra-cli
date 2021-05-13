package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.ResourceName;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.cli.service.utils.GoogleAiNotebooks;
import bio.terra.cloudres.google.notebooks.InstanceName;
import bio.terra.workspace.model.GcpAiNotebookInstanceAttributes;
import bio.terra.workspace.model.ResourceDescription;
import bio.terra.workspace.model.ResourceType;
import com.google.api.services.notebooks.v1.model.Instance;
import javax.annotation.Nullable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources describe" command. */
@CommandLine.Command(name = "describe", description = "Describe a resource.")
public class Describe extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin Format formatOption;

  /** Describe a resource. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(resourceNameOption.name);
    ResourceInfo resourceInfo = new ResourceInfo(resource, getInstance(resource));
    formatOption.printReturnValue(resourceInfo, ResourceInfo::printText);
  }

  @Nullable
  private Instance getInstance(ResourceDescription resource) {
    if (!resource.getMetadata().getResourceType().equals(ResourceType.AI_NOTEBOOK)) {
      return null;
    }
    GcpAiNotebookInstanceAttributes notebookAttributes =
        resource.getResourceAttributes().getGcpAiNotebookInstance();
    InstanceName instanceName =
        InstanceName.builder()
            .projectId(notebookAttributes.getProjectId())
            .location(notebookAttributes.getLocation())
            .instanceId(notebookAttributes.getInstanceId())
            .build();
    GoogleAiNotebooks notebooks =
        new GoogleAiNotebooks(globalContext.requireCurrentTerraUser().userCredentials);
    return notebooks.get(instanceName);
  }

  /** Bundle a ResourceDescription with additional resource tyep specific data for JSON printing. */
  // TODO(PF-729): Consider how to print more resource specific information.
  private static class ResourceInfo {
    private ResourceDescription resourceDescription;
    @Nullable private Instance aiNotebookInstance;

    public ResourceInfo(ResourceDescription resourceDescription, Instance aiNotebookInstance) {
      this.resourceDescription = resourceDescription;
      this.aiNotebookInstance = aiNotebookInstance;
    }

    public ResourceDescription getResourceDescription() {
      return resourceDescription;
    }

    public void setResourceDescription(ResourceDescription resourceDescription) {
      this.resourceDescription = resourceDescription;
    }

    public Instance getAiNotebookInstance() {
      return aiNotebookInstance;
    }

    public void setAiNotebookInstance(Instance instance) {
      this.aiNotebookInstance = instance;
    }

    public void printText() {
      PrintingUtils.printText(resourceDescription);
      printText(aiNotebookInstance);
    }

    /** Print the most interesting subset of the Instance. */
    private static void printText(Instance instance) {
      if (instance == null) {
        return;
      }
      OUT.println("Instance name: " + instance.getName());
      OUT.println("State:         " + instance.getState());
      OUT.println("Proxy URL:     " + instance.getProxyUri());
      OUT.println("Create time:   " + instance.getCreateTime());
    }
  }
}
