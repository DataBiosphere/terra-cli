package bio.terra.cli.command.groups;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.SamService;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups create" command. */
@Command(name = "create", description = "Create a new Terra group.")
public class Create implements Callable<Integer> {
  @CommandLine.Parameters(index = "0", description = "The name of the group")
  private String group;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    new SamService(globalContext.server, globalContext.requireCurrentTerraUser())
        .createGroup(group);

    System.out.println("Group " + group + " successfully created.");

    return 0;
  }
}
