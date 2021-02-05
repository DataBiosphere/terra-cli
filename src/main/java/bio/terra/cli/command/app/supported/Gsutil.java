package bio.terra.cli.command.app.supported;

import bio.terra.cli.apps.DockerAppsRunner;
import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gsutil" command. */
@Command(
    name = "gsutil",
    description = "Use the gsutil tool in the Terra workspace.",
    hidden = true)
public class Gsutil implements Callable<Integer> {

  @CommandLine.Unmatched private String[] cmdArgs;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    String fullCommand = DockerAppsRunner.buildFullCommand("gsutil", cmdArgs);

    // no need for any special setup or teardown logic since gsutil is already initialized when the
    // container starts
    String cmdOutput =
        new DockerAppsRunner(globalContext, workspaceContext).runToolCommand(fullCommand);
    System.out.println(cmdOutput);

    return 0;
  }
}
