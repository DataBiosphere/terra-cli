package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
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
      required = false,
      description = "Disable cache",
      defaultValue = "false")
  private Boolean disableCache;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace ws = Context.requireWorkspace();

    MountController mountController = MountControllerFactory.getMountController();
    if (MountController.workspaceDirExists()) {
      mountController.unmountResources();
    }
    mountController.mountResources(ws, disableCache);
    OUT.println("Mounted workspace resources.");
  }
}
