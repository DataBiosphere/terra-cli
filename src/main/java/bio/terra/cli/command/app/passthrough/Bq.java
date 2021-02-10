package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra bq" command. */
@Command(name = "bq", description = "Use the bq tool in the Terra workspace.", hidden = true)
public class Bq implements Callable<Integer> {

  @CommandLine.Unmatched private List<String> cmdArgs;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    String fullCommand = DockerAppsRunner.buildFullCommand("bq", cmdArgs);

    // no need for any special setup or teardown logic since bq is already initialized when the
    // container starts
    new DockerAppsRunner(globalContext, workspaceContext).runToolCommand(fullCommand);

    return 0;
  }
}
