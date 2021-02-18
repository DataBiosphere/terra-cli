package bio.terra.cli.command.groups;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.SamService;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups list-users" command. */
@Command(name = "list-users", description = "List the users in a group.")
public class ListUsers implements Callable<Integer> {
  @CommandLine.Parameters(index = "0", description = "The name of the group")
  private String group;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "The name of the policy: ${COMPLETION-CANDIDATES}")
  private SamService.GroupPolicy policy;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    List<String> users =
        new SamService(globalContext.server, globalContext.requireCurrentTerraUser())
            .listUsersInGroup(group, policy);

    for (String user : users) {
      System.out.println(user);
    }

    return 0;
  }
}
