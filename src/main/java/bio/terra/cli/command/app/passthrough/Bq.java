package bio.terra.cli.command.app.passthrough;

import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra bq" command. */
@Command(name = "bq", description = "Call bq in the Terra workspace.")
public class Bq extends ToolCommand {

  @Override
  public String getExecutableName() {
    return "bq";
  }

  @Override
  public String getInstallationUrl() {
    return "https://cloud.google.com/sdk/docs/install";
  }
}
