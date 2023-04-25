package bio.terra.cli.command.resource;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import picocli.CommandLine.Command;

/** This class corresponds to the "terra resource unmount" command. */
@Command(name = "unmount", description = "Unmounts all workspace bucket resources.")
public class Unmount extends BaseCommand {

  @Override
  protected void execute() {
    MountController mountController = MountControllerFactory.getMountController();
    if (MountController.workspaceDirExists()) {
      mountController.unmountResources();
      OUT.println("Unmounted workspace resources.");
    } else {
      OUT.println("There are no mounted workspace resources.");
    }
  }
}
