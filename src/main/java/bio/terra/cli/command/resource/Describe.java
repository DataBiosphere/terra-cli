package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.WsmBaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.serialization.userfacing.UFResource;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource describe" command. */
@CommandLine.Command(name = "describe", description = "Describe a resource.")
public class Describe extends WsmBaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Describe a resource. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    formatOption.printReturnValue(resource.serializeToCommand(), UFResource::print);
  }
}
