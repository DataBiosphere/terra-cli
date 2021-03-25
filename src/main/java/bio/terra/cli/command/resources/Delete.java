package bio.terra.cli.command.resources;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resources delete" command. */
@Command(name = "delete", description = "Delete an existing controlled resource.")
public class Delete extends BaseCommand {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the resource, scoped to the workspace.")
  private String name;

  /** Delete an existing controlled resource. */
  @Override
  protected void execute() {
    CloudResource resource =
        new WorkspaceManager(globalContext, workspaceContext).deleteControlledResource(name);
    OUT.println(resource.type + " successfully deleted: " + resource.cloudId);
    OUT.println("Workspace resource successfully removed: " + resource.name);
  }
}
