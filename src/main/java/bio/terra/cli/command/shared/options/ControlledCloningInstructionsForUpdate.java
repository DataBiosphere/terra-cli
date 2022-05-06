package bio.terra.cli.command.shared.options;

import bio.terra.workspace.model.CloningInstructionsEnum;
import picocli.CommandLine;

public class ControlledCloningInstructionsForUpdate {
  /**
   * Use this --cloning flog for controlled resources.
   *
   * <p>This class is meant to be used as a @CommandLine.Mixin.
   */

  // Can't have a default. This is used for update. Need `null` in case user does not want to
  // update cloning instructions.

  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}.")
  private CloningInstructionsEnum cloning;

  public CloningInstructionsEnum getCloning() {
    return cloning;
  }
}
