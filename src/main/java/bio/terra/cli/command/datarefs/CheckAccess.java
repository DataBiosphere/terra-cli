package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.helperclasses.CommandSetup;
import bio.terra.cli.command.helperclasses.FormatFlag;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs check-access" command. */
@CommandLine.Command(
    name = "check-access",
    description = "Check if you have access to a data reference.")
public class CheckAccess extends CommandSetup {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the data reference, scoped to the workspace.")
  private String name;

  @CommandLine.Mixin FormatFlag formatFlag;

  /** Check if the user and their proxy group have access to a data reference. */
  @Override
  protected void execute() {
    CloudResource dataReference =
        new WorkspaceManager(globalContext, workspaceContext).getDataReference(name);

    TerraUser currentTerraUser = globalContext.requireCurrentTerraUser();
    boolean userHasAccess = dataReference.checkAccessForUser(currentTerraUser, workspaceContext);
    boolean proxyGroupHasAccess =
        dataReference.checkAccessForPetSa(currentTerraUser, workspaceContext);

    CheckAccessReturnValue checkAccessReturnValue =
        new CheckAccessReturnValue(userHasAccess, proxyGroupHasAccess);
    formatFlag.printReturnValue(
        checkAccessReturnValue,
        returnValue -> {
          this.printText(returnValue);
        });
  }

  /** POJO class for printing out this command's output. */
  public static class CheckAccessReturnValue {
    // true if the user's email has acccess
    public final boolean userHasAccess;

    // true if the user's proxy group has access
    public final boolean proxyGroupHasAccess;

    public CheckAccessReturnValue(boolean userHasAccess, boolean proxyGroupHasAccess) {
      this.userHasAccess = userHasAccess;
      this.proxyGroupHasAccess = proxyGroupHasAccess;
    }
  }

  /**
   * Print this command's output in text format.
   *
   * @param returnValue command return value object
   */
  private void printText(CheckAccessReturnValue returnValue) {
    TerraUser currentTerraUser = globalContext.requireCurrentTerraUser();
    out.println(
        "User ("
            + currentTerraUser.terraUserEmail
            + ") DOES "
            + (returnValue.userHasAccess ? "" : "NOT ")
            + "have access to this data reference.");
    out.println(
        "User's pet SA in their proxy group ("
            + currentTerraUser.terraProxyGroupEmail
            + ") DOES "
            + (returnValue.proxyGroupHasAccess ? "" : "NOT ")
            + "have access to this data reference.");
  }
}
