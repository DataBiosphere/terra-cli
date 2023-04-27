package bio.terra.cli.command.resource;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import javax.annotation.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the "terra resource unmount" command. */
@Command(name = "unmount", description = "Unmounts all workspace bucket resources.")
public class Unmount extends BaseCommand {

  @CommandLine.Option(names = "--name", description = "Specify an individual resource to unmount.")
  private @Nullable String resourceName;

  @Override
  protected void execute() {
    MountController mountController = MountControllerFactory.getMountController();

    // Unmount only a single resource if specified, throws error if mount fails.
    if (resourceName != null) {
      mountController.unmountResource(resourceName);
      OUT.println("Unmounted resource " + resourceName + ".");
    }
    // Unmount all resources otherwise
    else if (MountController.workspaceDirExists()) {
      mountController.unmountResources();
      OUT.println("Unmounted workspace resources.");
    } else {
      OUT.println("There are no mounted workspace resources.");
    }
  }
}
