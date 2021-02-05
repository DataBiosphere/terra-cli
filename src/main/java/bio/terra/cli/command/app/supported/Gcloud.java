package bio.terra.cli.command.app.supported;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.ToolsManager;
import bio.terra.cli.app.supported.SupportedToolHelper;
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
    String fullCommand = SupportedToolHelper.buildFullCommand("gcloud", cmdArgs);

    // no need for any special setup or teardown logic since gcloud is already initialized when the
    // container starts
    String cmdOutput =
        new ToolsManager(globalContext, workspaceContext).runToolCommand(fullCommand);
    System.out.println(cmdOutput);

    return 0;
  }
}
