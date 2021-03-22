package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs delete" command. */
@CommandLine.Command(name = "delete", description = "Delete an existing data reference.")
public class Delete extends CommandWithFormatOptions<CloudResource> {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the data reference, scoped to the workspace.")
  private String name;

  @Override
  protected CloudResource execute() {
    return new WorkspaceManager(globalContext, workspaceContext).deleteDataReference(name);
  }

  @Override
  protected void printText(CloudResource returnValue) {
    out.println("Workspace data reference successfully deleted: " + returnValue.name);
  }
}
