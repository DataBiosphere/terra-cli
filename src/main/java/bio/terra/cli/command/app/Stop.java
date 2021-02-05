package bio.terra.cli.command.app;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.supported.SupportedApp;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app stop" command. */
@Command(name = "stop", description = "Stop a running application in the Terra workspace.")
public class Stop implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description = "name of the application to stop: ${COMPLETION-CANDIDATES}")
  private SupportedApp app;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    if (app.enableStopPattern) {
      new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
      app.getToolHelper(globalContext, workspaceContext).stop();
    }

    System.out.println("Application successfully stopped: " + app.toString());
    return 0;
  }
}
