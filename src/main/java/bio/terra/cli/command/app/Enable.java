package bio.terra.cli.command.app;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app enable" command. */
@Command(name = "enable", description = "Enable an application in the Terra workspace.")
public class Enable implements Callable<Integer> {

  @CommandLine.Parameters(
      index = "0",
      description = "name of the application to enable: ${COMPLETION-CANDIDATES}")
  private bio.terra.cli.apps.interfaces.Enable.EnableApp app;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    bio.terra.cli.apps.interfaces.Enable appHelper = app.getAppHelper();
    appHelper.setContext(globalContext, workspaceContext);
    appHelper.enable();

    // TODO: should we also pull the image on any call to enable?

    System.out.println("App successfully enabled: " + app.toString());
    return 0;
  }
}
