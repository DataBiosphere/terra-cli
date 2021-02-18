package bio.terra.cli.command.groups;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.SamService;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups describe" command. */
@Command(name = "describe", description = "Print the group email address.")
public class Describe implements Callable<Integer> {
  @CommandLine.Parameters(index = "0", description = "The name of the group")
  private String group;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    String groupEmail =
        new SamService(globalContext.server, globalContext.requireCurrentTerraUser())
            .getGroupEmail(group);

    System.out.println(groupEmail);

    return 0;
  }
}
