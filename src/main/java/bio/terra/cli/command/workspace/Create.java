package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.FormatOption;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.WorkspaceDescription;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace create" command. */
@Command(name = "create", description = "Create a new workspace.")
public class Create extends BaseCommand {

  @CommandLine.Option(names = "--name", required = false, description = "workspace display name")
  private String displayName;

  @CommandLine.Option(
      names = "--description",
      required = false,
      description = "workspace description")
  private String description;

  @CommandLine.Mixin FormatOption formatOption;

  /** Create a new workspace. */
  @Override
  protected void execute() {
    new WorkspaceManager(globalContext, workspaceContext).createWorkspace(displayName, description);
    new AuthenticationManager(globalContext, workspaceContext)
        .fetchPetSaCredentials(globalContext.requireCurrentTerraUser());
    formatOption.printReturnValue(workspaceContext.terraWorkspaceModel, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(WorkspaceDescription returnValue) {
    OUT.println("Workspace successfully created: " + workspaceContext.getWorkspaceId());
  }
}
