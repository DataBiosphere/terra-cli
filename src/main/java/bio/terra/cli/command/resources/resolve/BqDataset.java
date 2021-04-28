package bio.terra.cli.command.resources.resolve;

import static bio.terra.cli.service.WorkspaceManager.getBigQueryDatasetPath;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.GcpBigQueryDatasetAttributes;
import bio.terra.workspace.model.ResourceDescription;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resources resolve bq-dataset" command. */
@CommandLine.Command(
    name = "bq-dataset",
    description = "Resolve a Big Query dataset resource to its cloud id or path.")
public class BqDataset extends BaseCommand {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "Name of the resource, scoped to the workspace.")
  private String name;

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
  BqDataset.BqDatasetCloudIdArgGroup argGroup;

  static class BqDatasetCloudIdArgGroup {
    @CommandLine.Option(
        names = "--dataset-id",
        description = "Only return the Big Query dataset id.")
    private boolean datasetId;

    @CommandLine.Option(names = "--project-id", description = "Only return the GCP project id.")
    private boolean projectId;
  }

  @CommandLine.Mixin FormatOption formatOption;

  /** Resolve a Big Query dataset resource in the workspace to its cloud identifier. */
  @Override
  protected void execute() {
    ResourceDescription resource =
        new WorkspaceManager(globalContext, workspaceContext).getResource(name);
    GcpBigQueryDatasetAttributes bigQueryDatasetAttributes =
        resource.getResourceAttributes().getGcpBqDataset();

    // either return 'project.dataset', or only the project or dataset if one of the optional flags
    // is specified
    String cloudId;
    if (argGroup.datasetId) {
      cloudId = bigQueryDatasetAttributes.getDatasetId();
    } else if (argGroup.projectId) {
      cloudId = bigQueryDatasetAttributes.getProjectId();
    } else {
      cloudId = getBigQueryDatasetPath(resource);
    }
    formatOption.printReturnValue(cloudId);
  }
}
