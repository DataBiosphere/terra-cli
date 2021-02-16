package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.RoleBinding;
import bio.terra.workspace.model.RoleBindingList;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace list-users" command. */
@Command(name = "list-users", description = "List the users of the workspace.")
public class ListUsers implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    RoleBindingList roleBindings =
        new WorkspaceManager(globalContext, workspaceContext).listUsersOfWorkspace();
    for (RoleBinding roleBinding : roleBindings) {
      System.out.println(roleBinding.getRole());
      for (String member : roleBinding.getMembers()) {
        System.out.println("  " + member);
      }
    }
    return 0;
  }
}
