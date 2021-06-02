package bio.terra.cli.command.resources;

import bio.terra.cli.Context;
import bio.terra.cli.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.serialization.command.CommandResource;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources describe" command. */
@CommandLine.Command(name = "describe", description = "Describe a resource.")
public class Describe extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin Format formatOption;

  /** Describe a resource. */
  @Override
  protected void execute() {
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    formatOption.printReturnValue(
        resource.getResourceType().getCommandBuilder(resource).build(), CommandResource::print);
  }
}
