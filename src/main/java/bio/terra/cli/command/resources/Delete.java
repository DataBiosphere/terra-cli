package bio.terra.cli.command.resources;

import bio.terra.cli.Context;
import bio.terra.cli.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.serialization.command.CommandResource;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources delete" command. */
@CommandLine.Command(name = "delete", description = "Delete a resource from the workspace.")
public class Delete extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin Format formatOption;

  /** Delete a resource from the workspace. */
  @Override
  protected void execute() {
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    resource.delete();
    formatOption.printReturnValue(
        CommandResource.serializeFromInternal(resource), Delete::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(CommandResource returnValue) {
    OUT.println("Successfully deleted resource.");
    returnValue.print();
  }
}
