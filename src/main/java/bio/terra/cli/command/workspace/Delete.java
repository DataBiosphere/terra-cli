package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.service.WorkspaceManager;
import java.util.UUID;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace delete" command. */
@Command(name = "delete", description = "Delete an existing workspace.")
public class Delete extends BaseCommand {

  /** Delete an existing workspace. */
  @Override
  protected void execute() {
    UUID workspaceIdDeleted =
        new WorkspaceManager(globalContext, workspaceContext).deleteWorkspace();
    new AuthenticationManager(globalContext, workspaceContext)
        .deletePetSaCredentials(globalContext.requireCurrentTerraUser());

    OUT.println("Workspace successfully deleted: " + workspaceIdDeleted);
  }
}
