package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.Workspace;
import java.util.UUID;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace load" command. */
@Command(name = "load", description = "Load an existing workspace.")
public class Load extends BaseCommand {

  @CommandLine.Option(names = "--id", required = true, description = "workspace id")
  private UUID id;

  @CommandLine.Mixin Format formatOption;

  /** Load an existing workspace. */
  @Override
  protected void execute() {
    Workspace workspace = Workspace.load(id);
    new AuthenticationManager(globalContext, workspaceContext)
        .fetchPetSaCredentials(globalContext.requireCurrentTerraUser());
    formatOption.printReturnValue(workspace, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(Workspace returnValue) {
    OUT.println("Workspace successfully loaded.");
    returnValue.printText();
  }
}
