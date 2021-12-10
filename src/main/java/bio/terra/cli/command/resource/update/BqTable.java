package bio.terra.cli.command.resource.update;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceUpdate;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.resource.UFBqTable;
import picocli.CommandLine;

/** This class corresponds to the fourth-level "terra resource update bq-table" command. */
@CommandLine.Command(
    name = "bq-table",
    description = "Update a BigQuery data table.",
    showDefaultValues = true)
public class BqTable extends BaseCommand {
  @CommandLine.Mixin ResourceUpdate resourceUpdateOptions;
  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /** Update a BigQuery dataset in the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    // all update parameters are optional, but make sure at least one is specified
    if (!resourceUpdateOptions.isDefined()) {
      throw new UserActionableException("Specify at least one property to update.");
    }

    // get the resource and make sure it's the right type
    bio.terra.cli.businessobject.resource.BqTable resource =
        Context.requireWorkspace()
            .getResource(resourceUpdateOptions.resourceNameOption.name)
            .castToType(Resource.Type.BQ_TABLE);

    resource.updateReferenced(resourceUpdateOptions.populateMetadataFields().build());

    formatOption.printReturnValue(new UFBqTable(resource), BqTable::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(UFBqTable returnValue) {
    OUT.println("Successfully updated BigQuery data table.");
    returnValue.print();
  }
}
