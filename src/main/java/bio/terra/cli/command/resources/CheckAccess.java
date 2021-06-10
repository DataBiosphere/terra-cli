package bio.terra.cli.command.resources;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources check-access" command. */
@CommandLine.Command(
    name = "check-access",
    description = "Check if you have access to a referenced resource.")
public class CheckAccess extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /**
   * Check if the user and their proxy group have access to a referenced resource in the workspace.
   */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    boolean userHasAccess = resource.checkAccess(Resource.CheckAccessCredentials.USER);
    boolean proxyGroupHasAccess = resource.checkAccess(Resource.CheckAccessCredentials.PET_SA);

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
    User currentUser = Context.requireUser();
    OUT.println(
        "User ("
            + currentUser.getEmail()
            + ") DOES "
            + (returnValue.userHasAccess ? "" : "NOT ")
            + "have access to this resource.");
    OUT.println(
        "User's pet SA in their proxy group ("
            + currentUser.getProxyGroupEmail()
            + ") DOES "
            + (returnValue.proxyGroupHasAccess ? "" : "NOT ")
            + "have access to this resource.");
  }
}
