package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(name = "gcloud", description = "Call gcloud in the Terra workspace.")
public class Gcloud extends BaseCommand {

  @CommandLine.Unmatched private List<String> command = new ArrayList<>();

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    // no need for any special setup or teardown logic since gcloud is already initialized when the
    // container starts
    command.add(0, "gcloud");
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }
}
