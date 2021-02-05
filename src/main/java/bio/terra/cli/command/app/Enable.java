package bio.terra.cli.command.app;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.supported.SupportedApp;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app enable" command. */
@Command(name = "enable", description = "Enable an application in the Terra workspace.")
public class Enable implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description = "name of the application to enable: ${COMPLETION-CANDIDATES}")
  private SupportedApp app;

  @Override
  public Integer call() {
    if (app.enableStopPattern) {
      GlobalContext globalContext = GlobalContext.readFromFile();
      WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

      new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
      app.getToolHelper(globalContext, workspaceContext).enable();
    }

    // TODO: should we also pull the image on any call to enable?

    System.out.println("App successfully enabled: " + app.toString());
    return 0;
  }
}
