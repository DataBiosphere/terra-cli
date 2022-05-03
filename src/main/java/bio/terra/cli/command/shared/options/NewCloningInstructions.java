package bio.terra.cli.command.shared.options;

import bio.terra.workspace.model.CloningInstructionsEnum;
import picocli.CommandLine;

public class NewCloningInstructions {
  /**
   * Command helper class that defines the project id and dataset id when updating a BigQuery
   * datasets or BigQuery data table.
   *
   * <p>This class is meant to be used as a @CommandLine.Mixin.
   */

  // Cloning option must have a different default for referenced resources (REFERENCE) than
  // for controlled resources (RESOURCE).
  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}.")
  private CloningInstructionsEnum cloning;

  public CloningInstructionsEnum getCloning() {
    return cloning;
  }
}
