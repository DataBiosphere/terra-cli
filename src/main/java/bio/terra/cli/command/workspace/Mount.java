package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.WorkspaceDescription;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace mount" command. */
@Command(name = "mount", description = "Mount an existing workspace to the current directory.")
public class Mount extends BaseCommand {

  @CommandLine.Parameters(index = "0", description = "workspace id")
  private String workspaceId;

  @CommandLine.Mixin Format formatOption;

  /** Mount an existing workspace. */
  @Override
  protected void execute() {
    new WorkspaceManager(globalContext, workspaceContext).mountWorkspace(workspaceId);
    new AuthenticationManager(globalContext, workspaceContext)
        .fetchPetSaCredentials(globalContext.requireCurrentTerraUser());

    formatOption.printReturnValue(workspaceContext.terraWorkspaceModel, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(WorkspaceDescription returnValue) {
    OUT.println("Workspace successfully mounted.");
    PrintingUtils.printWorkspace(workspaceContext);
  }
}
