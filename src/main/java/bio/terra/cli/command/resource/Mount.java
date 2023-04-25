package bio.terra.cli.command.resource;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the "terra resource mount" command. */
@Command(name = "mount", description = "Mounts all workspace bucket resources.")
public class Mount extends BaseCommand {

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /**
   * Disabling caching
   *
   * <p>For gcs buckets, pass `--stat-cache-ttl 0` and `--type-cache-ttl 0` to `gcsfuse` to disable
   * file metadata caching and file type caching. See
   * https://github.com/GoogleCloudPlatform/gcsfuse/blob/master/docs/semantics.md#caching.
   */
  @CommandLine.Option(
      names = "--disable-cache",
      description = "Disables file metadata caching.",
      defaultValue = "false")
  private Boolean disableCache;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    MountController mountController = MountControllerFactory.getMountController();
    if (MountController.workspaceDirExists()) {
      mountController.unmountResources();
    }
    int errors = mountController.mountResources(disableCache);
    if (errors != 0) {
      OUT.println("One or more resources failed to unmount.");
    } else {
      OUT.println("Successfully mounted workspace bucket resources.");
    }
  }
}
