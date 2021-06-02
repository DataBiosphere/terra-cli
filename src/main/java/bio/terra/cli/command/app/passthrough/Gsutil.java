package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.Context;
import bio.terra.cli.command.shared.BaseCommand;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gsutil" command. */
@Command(name = "gsutil", description = "Call gsutil in the Terra workspace.")
public class Gsutil extends BaseCommand {

  @CommandLine.Unmatched private List<String> command = new ArrayList<>();

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    // no need for any special setup or teardown logic since gsutil is already initialized when the
    // container starts
    command.add(0, "gsutil");
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }
}
