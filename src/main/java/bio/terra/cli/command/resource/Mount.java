package bio.terra.cli.command.resource;

import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.utils.mount.MountController;
import bio.terra.cli.utils.mount.MountControllerFactory;
import javax.annotation.Nullable;
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

  /**
   * Whether to mount the resource(s) as read-only or read-write, overriding default mount
   * permissions.
   *
   * <p>By default, for controlled resources created by the calling user, the mount is always
   * read-write regardless of the value of this flag.
   *
   * <p>For other resources, this flag will determine whether the mount is read-only or read-write,
   * and defaults to read-only.
   */
  @CommandLine.Option(names = "--read-only", description = "Mount with only read permissions.")
  private @Nullable Boolean readOnly;

  /** Optionally mount an individual resource instead of all resources */
  @CommandLine.Option(names = "--name", description = "Specify an individual resource to mount.")
  private @Nullable String resourceName;

  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    MountController mountController = MountControllerFactory.getMountController();

    // Mount an individual resource if resourceName is provided.
    // Throws error if mount fails.
    if (resourceName != null) {
      mountController.unmountResource(resourceName, /*silent=*/ true);
      mountController.mountResource(resourceName, disableCache, readOnly);
      OUT.println("Successfully mounted resource " + resourceName + ".");
    }
    // Mount all resources otherwise.
    else {
      if (MountController.workspaceDirExists()) {
        mountController.unmountResources();
      }
      int errors = mountController.mountResources(disableCache, readOnly);
      if (errors == 0) {
        OUT.println("Successfully mounted workspace bucket resources.");
      } else {
        throw new UserActionableException("One or more resources failed to mount.");
      }
    }
  }
}
