package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.ResourceName;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources check-access" command. */
@CommandLine.Command(
    name = "check-access",
    description = "Check if you have access to a referenced resource.")
public class CheckAccess extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin Format formatOption;

  /**
   * Check if the user and their proxy group have access to a referenced resource in the workspace.
   */
  @Override
  protected void execute() {
    WorkspaceManager workspaceManager = new WorkspaceManager(globalContext, workspaceContext);
    ResourceDescription resource = workspaceManager.getResource(resourceNameOption.name);

    // call the appropriate check-access function for the resource
    // there is different logic for checking the access of each resource type, but all require only
    // the user/pet SA credentials and resource definition, so calling them looks very similar from
    // the CLI user's perspective.
    // still, it may be better to break this command into sub-commands for each resource type. that
    // would allow different options per resource, e.g. checking different resource-specific
    // permissions.
    // TODO (PF-717): revisit this command once the WSM endpoints are complete
    boolean userHasAccess, proxyGroupHasAccess;
    switch (resource.getMetadata().getResourceType()) {
      case GCS_BUCKET:
        userHasAccess =
            workspaceManager.checkAccessToReferencedGcsBucket(
                resource, WorkspaceManager.CheckAccessCredentials.USER);
        proxyGroupHasAccess =
            workspaceManager.checkAccessToReferencedGcsBucket(
                resource, WorkspaceManager.CheckAccessCredentials.PET_SA);
        break;
      case BIG_QUERY_DATASET:
        userHasAccess =
            workspaceManager.checkAccessToReferencedBigQueryDataset(
                resource, WorkspaceManager.CheckAccessCredentials.USER);
        proxyGroupHasAccess =
            workspaceManager.checkAccessToReferencedBigQueryDataset(
                resource, WorkspaceManager.CheckAccessCredentials.PET_SA);
        break;
      default:
        throw new UnsupportedOperationException(
            "Resource type not supported: " + resource.getMetadata().getResourceType());
    }

    CheckAccess.CheckAccessReturnValue checkAccessReturnValue =
        new CheckAccess.CheckAccessReturnValue(userHasAccess, proxyGroupHasAccess);
    formatOption.printReturnValue(checkAccessReturnValue, this::printText);
  }

  /** POJO class for printing out this command's output. */
  private static class CheckAccessReturnValue {
    // true if the user's email has acccess
    public final boolean userHasAccess;

    // true if the user's proxy group has access
    public final boolean proxyGroupHasAccess;

    public CheckAccessReturnValue(boolean userHasAccess, boolean proxyGroupHasAccess) {
      this.userHasAccess = userHasAccess;
      this.proxyGroupHasAccess = proxyGroupHasAccess;
    }
  }

  /** Print this command's output in text format. */
  public void printText(CheckAccess.CheckAccessReturnValue returnValue) {
    TerraUser currentTerraUser = globalContext.requireCurrentTerraUser();
    OUT.println(
        "User ("
            + currentTerraUser.terraUserEmail
            + ") DOES "
            + (returnValue.userHasAccess ? "" : "NOT ")
            + "have access to this resource.");
    OUT.println(
        "User's pet SA in their proxy group ("
            + currentTerraUser.terraProxyGroupEmail
            + ") DOES "
            + (returnValue.proxyGroupHasAccess ? "" : "NOT ")
            + "have access to this resource.");
  }
}
