package bio.terra.cli.command.app;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.ToolsManager;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app execute" command. */
@Command(
    name = "execute",
    description =
        "[FOR DEBUG] Execute a command in the application container for the Terra workspace, with no setup.")
public class Execute implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", paramLabel = "command", description = "command to execute")
  private String cmd;

  @CommandLine.Parameters(
      index = "1..*",
      paramLabel = "arguments",
      description = "command arguments")
  private String[] cmdArgs;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    String fullCommand = cmd;
    if (cmdArgs != null && cmdArgs.length > 0) {
      final String argSeparator = " ";
      fullCommand += argSeparator + String.join(argSeparator, cmdArgs);
    }
    String cmdOutput =
        new ToolsManager(globalContext, workspaceContext).runToolCommand(fullCommand);
    System.out.println(cmdOutput);

    System.out.println("App command successfully executed: " + fullCommand);

    return 0;
  }
}
