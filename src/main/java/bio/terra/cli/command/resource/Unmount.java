package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.utils.MountUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the "terra resource unmount" command. */
@Command(name = "unmount", description = "Unmounts workspace resources.")
public class Unmount extends BaseCommand {

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** Execute */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Workspace ws = Context.requireWorkspace();

    if (MountUtils.workspaceDirExists()) {
      MountUtils.unmountResources(ws);
      OUT.println("Unmounted workspace resources.");
    } else {
      OUT.println("There are no mounted workspace resources.");
    }
  }
}
