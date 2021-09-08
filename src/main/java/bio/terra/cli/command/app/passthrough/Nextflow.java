package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra nextflow" command. */
@Command(name = "nextflow", description = "Call nextflow in the Terra workspace.")
public class Nextflow extends BaseCommand {

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Unmatched private List<String> command = new ArrayList<>();
  private static final Logger logger = LoggerFactory.getLogger(Nextflow.class);

  /**
   * Enable the user's pet SA in the workspace context, then pass the command through to the CLI
   * Docker image.
   */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    String petEmail = Workspace.enablePet();
    logger.info("Enabled usage of for pet {} for Nextflow", petEmail);
    command.add(0, "nextflow");
    Map<String, String> envVars = new HashMap<>();
    envVars.put("NXF_MODE", "google");
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command, envVars);
  }
}
