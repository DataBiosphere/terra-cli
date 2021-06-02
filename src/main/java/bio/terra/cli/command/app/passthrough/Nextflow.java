package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.Context;
import bio.terra.cli.command.shared.BaseCommand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra nextflow" command. */
@Command(name = "nextflow", description = "Call nextflow in the Terra workspace.")
public class Nextflow extends BaseCommand {

  @CommandLine.Unmatched private List<String> command = new ArrayList<>();

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    command.add(0, "nextflow");
    Map<String, String> envVars = new HashMap<>();
    envVars.put("NXF_MODE", "google");
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command, envVars);
  }
}
