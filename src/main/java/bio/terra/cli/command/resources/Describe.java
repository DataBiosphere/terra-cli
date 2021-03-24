package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resources describe" command. */
@Command(name = "describe", description = "Describe an existing controlled resource.")
public class Describe extends BaseCommand {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the resource, scoped to the workspace.")
  private String name;

  @CommandLine.Mixin FormatOption formatOption;

  /** Describe an existing controlled resource. */
  @Override
  protected void execute() {
    CloudResource resource =
        new WorkspaceManager(globalContext, workspaceContext).getControlledResource(name);
    formatOption.printReturnValue(resource, Describe::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(CloudResource returnValue) {
    OUT.println("Name: " + returnValue.name);
    OUT.println("Type: " + returnValue.type);
    OUT.println("Cloud Id: " + returnValue.cloudId);
  }
}
