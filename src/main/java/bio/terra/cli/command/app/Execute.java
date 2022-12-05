package bio.terra.cli.command.app;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra app execute" command. */
@Command(
    name = "execute",
    description =
        "[FOR DEBUG] Execute a command in the application container for the Terra workspace, with no setup.")
public class Execute extends BaseCommand {
  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Parameters(
      index = "0",
      paramLabel = "command",
      description = "Command to execute, including arguments",
      arity = "1..*")
  private List<String> command;

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }
}
