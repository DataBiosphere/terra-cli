package bio.terra.cli.command.config.set;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format.FormatOptions;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set format" command. */
@Command(name = "format", description = "Set the default output format option.")
public class Format extends BaseCommand {
  @CommandLine.Parameters(
      index = "0",
      description = "Output format option: ${COMPLETION-CANDIDATES}.")
  private FormatOptions format;

  @Override
  protected void execute() {
    Config config = Context.getConfig();
    FormatOptions previousFormatOption = config.getFormat();
    config.setFormat(format);

    OUT.println(
        "Output format is "
            + config.getFormat()
            + " ("
            + (config.getFormat() == previousFormatOption ? "UNCHANGED" : "CHANGED")
            + ").");
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
