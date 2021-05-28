package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.command.helperclasses.BaseCommand;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra bq" command. */
@Command(name = "bq", description = "Call bq in the Terra workspace.")
public class Bq extends BaseCommand {

  @CommandLine.Unmatched private List<String> command = new ArrayList<>();

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    // no need for any special setup or teardown logic since bq is already initialized when the
    // container starts
    command.add(0, "bq");
    globalContext.commandRunnerOption.getCommandRunner().runToolCommand(command);
  }
}
