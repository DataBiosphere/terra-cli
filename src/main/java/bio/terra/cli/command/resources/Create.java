package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resources create" command. */
@Command(name = "create", description = "Create a new controlled resource.")
public class Create extends BaseCommand {

  @CommandLine.Option(
      names = "--type",
      required = true,
      description = "The type of resource to create: ${COMPLETION-CANDIDATES}")
  private CloudResource.Type type;

  @CommandLine.Option(
      names = "--name",
      required = true,
      description =
          "The name of the resource, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
  private String name;

  @CommandLine.Mixin FormatOption formatOption;

  /** Create a new controlled resource. */
  @Override
  protected void execute() {
    CloudResource resource =
        new WorkspaceManager(globalContext, workspaceContext).createControlledResource(type, name);
    formatOption.printReturnValue(resource, Create::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(CloudResource returnValue) {
    OUT.println(returnValue.type + " successfully created: " + returnValue.cloudId);
    OUT.println("Workspace resource successfully added: " + returnValue.name);
  }
}
