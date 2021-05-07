package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.command.helperclasses.options.ResourceName;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resources describe" command. */
@CommandLine.Command(name = "describe", description = "Describe a resource.")
public class Describe extends BaseCommand {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin Format formatOption;

  /** Describe a resource. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(resourceNameOption.name);
    formatOption.printReturnValue(resource, PrintingUtils::printText);
  }
}
