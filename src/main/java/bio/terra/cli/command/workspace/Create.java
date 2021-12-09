package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.WorkspaceNameAndDescription;
import bio.terra.cli.serialization.userfacing.UFWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace create" command. */
@Command(name = "create", description = "Create a new workspace.")
public class Create extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Create.class);

  @CommandLine.Mixin WorkspaceNameAndDescription workspaceNameAndDescription;

  @CommandLine.Mixin Format formatOption;

  /** Create a new workspace. */
  @Override
  protected void execute() {
    logger.debug("terra workspace create");
    Workspace workspace =
        Workspace.create(
            workspaceNameAndDescription.displayName, workspaceNameAndDescription.description);
    formatOption.printReturnValue(new UFWorkspace(workspace), this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(UFWorkspace returnValue) {
    OUT.println("Workspace successfully created.");
    returnValue.print();
  }
}
