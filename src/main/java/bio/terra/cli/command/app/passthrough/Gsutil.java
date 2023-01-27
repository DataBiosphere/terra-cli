package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.utils.CommandUtils;
import bio.terra.workspace.model.CloudPlatform;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gsutil" command. */
@Command(name = "gsutil", description = "Call gsutil in the Terra workspace.")
public class Gsutil extends ToolCommand {
  @Override
  public String getExecutableName() {
    return "gsutil";
  }

  @Override
  public String getInstallationUrl() {
    return "https://cloud.google.com/sdk/docs/install";
  }

  @Override
  protected void executeImpl() {
    workspaceOption.overrideIfSpecified();
    CommandUtils.checkWorkspaceSupport(CloudPlatform.GCP);

    super.executeImpl();
  }
}
