package bio.terra.cli.command.app.passthrough;

import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(name = "gcloud", description = "Call gcloud in the Terra workspace.")
public class Gcloud extends ToolCommand {

  @Override
  public String getExecutableName() {
    return "gcloud";
  }

  @Override
  public String getInstallationUrl() {
    return "https://cloud.google.com/sdk/docs/install";
  }
}
