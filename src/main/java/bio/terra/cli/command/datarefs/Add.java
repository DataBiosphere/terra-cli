package bio.terra.cli.command.datarefs;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.service.WorkspaceManager;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs add" command. */
@CommandLine.Command(name = "add", description = "Add a new data reference.")
public class Add extends BaseCommand {

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
      description =
          "The cloud id of the data reference. (e.g. for buckets gs://my-bucket', for BigQuery datasets 'projectId.datasetId')")
  private String uri;

  @CommandLine.Mixin FormatOption formatOption;

  /** Add a new data reference to the workspace. */
  @Override
  protected void execute() {
    CloudResource addDataRefReturnValue =
        new WorkspaceManager(globalContext, workspaceContext).addDataReference(type, name, uri);
    formatOption.printReturnValue(addDataRefReturnValue, Add::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(CloudResource returnValue) {
    OUT.println(
        "Workspace data reference successfully added: "
            + returnValue.name
            + " ("
            + returnValue.cloudId
            + ")");
  }
}
