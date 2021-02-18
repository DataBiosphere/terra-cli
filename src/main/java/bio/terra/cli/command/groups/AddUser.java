package bio.terra.cli.command.groups;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.SamService;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups add-user" command. */
@Command(name = "add-user", description = "Add a user to a group.")
public class AddUser implements Callable<Integer> {
  @CommandLine.Parameters(index = "0", description = "The email of the user.")
  private String user;

  @CommandLine.Option(names = "--group", required = true, description = "The name of the group")
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
    new SamService(globalContext.server, globalContext.requireCurrentTerraUser())
        .addUserToGroup(group, policy, user);

    System.out.println("User " + user + " successfully added to group " + group + ".");
    return 0;
  }
}
