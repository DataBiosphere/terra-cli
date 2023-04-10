package bio.terra.cli.command.resource;

import static bio.terra.cli.utils.MountUtils.mountResources;
import static bio.terra.cli.utils.MountUtils.unmountResources;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the "terra resource mount" command. */
@Command(name = "mount", description = "Mounts workspace resources.")
public class Mount extends BaseCommand {

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  @CommandLine.Option(names = "--disable-cache", required = false, description = "Disable cache")
  private Boolean disableCache;

  @CommandLine.Option(
      names = "--read-only",
      required = false,
      description = "Mount with only read permission")
  private Boolean readOnly;

  /** Execute */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace ws = Context.requireWorkspace();

    unmountResources(ws);
    mountResources(ws);
  }
}
