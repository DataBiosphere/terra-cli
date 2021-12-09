package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(name = "gcloud", description = "Call gcloud in the Terra workspace.")
public class Gcloud extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Gcloud.class);

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Unmatched private List<String> command = new ArrayList<>();

  /** Pass the command through to the CLI Docker image. */
  @Override
  protected void execute() {
    logger.debug("terra gcloud");
    workspaceOption.overrideIfSpecified();
    // no need for any special setup or teardown logic since gcloud is already initialized when the
    // container starts
    command.add(0, "gcloud");
    Context.getConfig().getCommandRunnerOption().getRunner().runToolCommand(command);
  }
}
