package bio.terra.cli.command.app;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app stop" command. */
@Command(name = "stop", description = "Stop a running application in the Terra workspace.")
public class Stop implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description = "name of the application to stop: ${COMPLETION-CANDIDATES}")
  private bio.terra.cli.apps.interfaces.Stop.StopApp app;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    bio.terra.cli.apps.interfaces.Stop appHelper = app.getAppHelper();
    appHelper.setContext(globalContext, workspaceContext);
    appHelper.stop();

    System.out.println("Application successfully stopped: " + app.toString());
    return 0;
  }
}
