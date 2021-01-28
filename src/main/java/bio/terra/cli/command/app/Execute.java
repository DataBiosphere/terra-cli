package bio.terra.cli.command.app;

import bio.terra.cli.app.AppsManager;
import bio.terra.cli.model.GlobalContext;
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
  public Integer call() throws InterruptedException {
    GlobalContext globalContext = GlobalContext.readFromFile();

    String fullCommand = cmd;
    if (cmdArgs != null && cmdArgs.length > 0) {
      final String argSeparator = " ";
      fullCommand += argSeparator + String.join(argSeparator, cmdArgs);
    }
    String cmdOutput = new AppsManager(globalContext).runAppCommand(fullCommand);
    System.out.println(cmdOutput);

    System.out.println("App command execution successful. (" + fullCommand + ")");

    return 0;
  }
}
