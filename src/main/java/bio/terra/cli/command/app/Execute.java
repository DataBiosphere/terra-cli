package bio.terra.cli.command.app;

import bio.terra.cli.command.helperclasses.BaseCommand;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app execute" command. */
@Command(
    name = "execute",
    description =
        "[FOR DEBUG] Execute a command in the application container for the Terra workspace, with no setup.")
public class Execute extends BaseCommand {

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "command",
      description = "Command to execute, including arguments",
      arity = "1..*")
  private List<String> command;

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    globalContext.getRunner(workspaceContext).runToolCommand(command);
  }
}
