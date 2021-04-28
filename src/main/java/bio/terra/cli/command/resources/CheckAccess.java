package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources check-access" command. */
@CommandLine.Command(
    name = "check-access",
    description = "Check if you have access to a referenced resource.")
public class CheckAccess extends BaseCommand {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "Name of the resource, scoped to the workspace.")
  private String name;

  @CommandLine.Mixin FormatOption formatOption;

  /**
   * Check if the user and their proxy group have access to a referenced resource in the workspace.
   */
  @Override
  protected void execute() {
    WorkspaceManager workspaceManager = new WorkspaceManager(globalContext, workspaceContext);
    boolean userHasAccess, proxyGroupHasAccess;

    ResourceDescription resource = workspaceManager.getResource(name);
    switch (resource.getMetadata().getResourceType()) {
      case GCS_BUCKET:
        userHasAccess = workspaceManager.checkAccessToReferencedGcsBucket(resource, false);
        proxyGroupHasAccess = workspaceManager.checkAccessToReferencedGcsBucket(resource, true);
        break;
      case BIG_QUERY_DATASET:
        userHasAccess = workspaceManager.checkAccessToReferencedBigQueryDataset(resource, false);
        proxyGroupHasAccess =
            workspaceManager.checkAccessToReferencedBigQueryDataset(resource, true);
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
