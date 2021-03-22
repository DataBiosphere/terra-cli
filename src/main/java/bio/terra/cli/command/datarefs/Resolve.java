package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs resolve" command. */
@CommandLine.Command(
    name = "resolve",
    description = "Resolve a data reference to its cloud id or path.")
public class Resolve extends CommandWithFormatOptions<String> {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the data reference, scoped to the workspace.")
  private String name;

  @Override
  protected String execute() {
    CloudResource dataReference =
        new WorkspaceManager(globalContext, workspaceContext).getDataReference(name);
    return dataReference.cloudId;
  }
}
