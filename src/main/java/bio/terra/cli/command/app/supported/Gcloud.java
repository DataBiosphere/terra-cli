package bio.terra.cli.command.app.supported;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.DockerToolsManager;
import bio.terra.cli.app.supported.SupportedAppHelper;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(
    name = "gcloud",
    description = "Use the gcloud tool in the Terra workspace.",
    hidden = true)
public class Gcloud implements Callable<Integer> {

  @CommandLine.Unmatched private String[] cmdArgs;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    String fullCommand = SupportedAppHelper.buildFullCommand("gcloud", cmdArgs);

    // no need for any special setup or teardown logic since gcloud is already initialized when the
    // container starts
    new DockerToolsManager(globalContext, workspaceContext).runToolCommand(fullCommand);

    return 0;
  }
}
