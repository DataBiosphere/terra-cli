package bio.terra.cli.command.spend;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.SpendProfileManagerService;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend enable" command. */
@Command(
    name = "enable",
    description = "Enable use of the Workspace Manager default spend profile for a user or group.")
public class Enable implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "The email of the user or group.")
  private String email;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "The name of the policy: ${COMPLETION-CANDIDATES}")
  private SpendProfileManagerService.SpendProfilePolicy policy;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    new SpendProfileManagerService(globalContext.server, globalContext.requireCurrentTerraUser())
        .enableUserForDefaultSpendProfile(policy, email);

    System.out.println(
        "Email " + email + " successfully enabled on the Workspace Manager default spend profile.");
    return 0;
  }
}
