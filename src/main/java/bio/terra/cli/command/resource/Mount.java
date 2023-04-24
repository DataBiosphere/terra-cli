package bio.terra.cli.command.resource;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the "terra resource mount" command. */
@Command(name = "mount", description = "Mounts workspace resources.")
public class Mount extends BaseCommand {

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Option(
      names = "--disable-cache",
      description = "Disable cache",
      defaultValue = "false")
  private Boolean disableCache;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    MountController mountController = MountControllerFactory.getMountController();
    if (MountController.workspaceDirExists()) {
      mountController.unmountResources();
    }
    mountController.mountResources(disableCache);
    OUT.println("Mounted workspace resources.");
  }
}
