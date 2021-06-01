package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.ResourceName;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.Resource;
import bio.terra.cli.context.TerraUser;
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
    Resource resource =
        GlobalContext.get().requireCurrentWorkspace().getResource(resourceNameOption.name);
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
    TerraUser currentTerraUser = globalContext.requireCurrentTerraUser();
    OUT.println(
        "User ("
            + currentTerraUser.getEmail()
            + ") DOES "
            + (returnValue.userHasAccess ? "" : "NOT ")
            + "have access to this resource.");
    OUT.println(
        "User's pet SA in their proxy group ("
            + currentTerraUser.getProxyGroupEmail()
            + ") DOES "
            + (returnValue.proxyGroupHasAccess ? "" : "NOT ")
            + "have access to this resource.");
  }
}
