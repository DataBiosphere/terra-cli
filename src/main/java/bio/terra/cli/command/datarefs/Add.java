package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs add" command. */
@CommandLine.Command(name = "add", description = "Add a new data reference.")
public class Add extends CommandWithFormatOptions<CloudResource> {

  @CommandLine.Option(
      names = "--type",
      required = true,
      description = "The type of data reference to create: ${COMPLETION-CANDIDATES}")
  private CloudResource.Type type;

  @CommandLine.Option(
      names = "--name",
      required = true,
      description =
          "The name of the data reference, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
  private String name;

  @CommandLine.Option(
      names = "--uri",
      required = true,
      description = "The bucket path (e.g. gs://my-bucket)")
  private String uri;

  @Override
  protected CloudResource execute() {
    return new WorkspaceManager(globalContext, workspaceContext).addDataReference(type, name, uri);
  }

  @Override
  protected void printText(CloudResource returnValue) {
    out.println(
        "Workspace data reference successfully added: "
            + returnValue.name
            + " ("
            + returnValue.cloudId
            + ")");
  }
}
