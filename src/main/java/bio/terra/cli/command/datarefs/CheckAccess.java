package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.TerraUser;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs check-access" command. */
@CommandLine.Command(
    name = "check-access",
    description = "Check if you have access to a data reference.")
public class CheckAccess extends CommandWithFormatOptions<CheckAccess.CheckAccessReturnValue> {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the data reference, scoped to the workspace.")
  private String name;

  private TerraUser currentTerraUser;

  @Override
  protected CheckAccessReturnValue execute() {
    CloudResource dataReference =
        new WorkspaceManager(globalContext, workspaceContext).getDataReference(name);

    currentTerraUser = globalContext.requireCurrentTerraUser();
    boolean userHasAccess = dataReference.checkAccessForUser(currentTerraUser, workspaceContext);
    boolean proxyGroupHasAccess =
        dataReference.checkAccessForPetSa(currentTerraUser, workspaceContext);

    return new CheckAccessReturnValue(userHasAccess, proxyGroupHasAccess);
  }

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

  @Override
  protected void printText(CheckAccessReturnValue returnValue) {
    System.out.println(
        "User ("
            + currentTerraUser.terraUserEmail
            + ") DOES "
            + (returnValue.userHasAccess ? "" : "NOT ")
            + "have access to this data reference.");
    System.out.println(
        "User's pet SA in their proxy group ("
            + currentTerraUser.terraProxyGroupEmail
            + ") DOES "
            + (returnValue.proxyGroupHasAccess ? "" : "NOT ")
            + "have access to this data reference.");
  }
}
