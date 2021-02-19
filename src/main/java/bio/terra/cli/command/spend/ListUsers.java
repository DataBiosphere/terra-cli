package bio.terra.cli.command.spend;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.SpendProfileManagerService;
import java.util.List;
import java.util.concurrent.Callable;
import org.broadinstitute.dsde.workbench.client.sam.model.AccessPolicyResponseEntry;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra spend list-users" command. */
@Command(
    name = "list-users",
    description = "List the users enabled on the Workspace Manager default spend profile.")
public class ListUsers implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    List<AccessPolicyResponseEntry> policies =
        new SpendProfileManagerService(
                globalContext.server, globalContext.requireCurrentTerraUser())
            .listUsersOfDefaultSpendProfile();

    for (AccessPolicyResponseEntry policy : policies) {
      System.out.println(policy.getPolicyName().toUpperCase());
      for (String member : policy.getPolicy().getMemberEmails()) {
        System.out.println("  " + member);
      }
    }

    return 0;
  }
}
