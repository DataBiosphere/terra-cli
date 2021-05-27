package bio.terra.cli.command.workspace;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.Workspace;
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

  @CommandLine.Mixin Format formatOption;

  /** Create a new workspace. */
  @Override
  protected void execute() {
    Workspace workspace = Workspace.create(displayName, description);
    new AuthenticationManager(globalContext, workspaceContext)
        .fetchPetSaCredentials(globalContext.requireCurrentTerraUser());
    formatOption.printReturnValue(workspace, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(Workspace returnValue) {
    OUT.println("Workspace successfully created.");
    returnValue.printText();
  }
}
