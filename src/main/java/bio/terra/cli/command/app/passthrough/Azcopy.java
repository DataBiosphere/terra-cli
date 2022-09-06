package bio.terra.cli.command.app.passthrough;

import picocli.CommandLine;

/** This class corresponds to the second-level "terra azcopy" command. */
@CommandLine.Command(name = "azcopy", description = "Call azcopy in the Terra workspace.")
public class Azcopy extends ToolCommand {
  @Override
  public String getExecutableName() {
    return "azcopy";
  }

  @Override
  public String getInstallationUrl() {
    return "https://docs.microsoft.com/en-us/azure/storage/common/storage-use-azcopy-v10";
  }
}
