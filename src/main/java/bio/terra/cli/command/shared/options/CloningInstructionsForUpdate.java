package bio.terra.cli.command.shared.options;

import bio.terra.workspace.model.CloningInstructionsEnum;
import picocli.CommandLine;

public class CloningInstructionsForUpdate {
  /**
   * Use this --new-cloning flag when updating resources.
   *
   * <p>This class is meant to be used as a @CommandLine.Mixin.
   */

  // Can't have a default. This is used for update. Need `null` in case user does not want to
  // update cloning instructions.
  @CommandLine.Option(
      names = "--new-cloning",
      // For what is allowed, see:
      // https://github.com/DataBiosphere/terra-workspace-manager/blob/main/service/src/main/java/bio/terra/workspace/service/resource/ResourceValidationUtils.java#L448
      description =
          "Instructions for handling when cloning the workspace: COPY_NOTHING, COPY_DEFINITION, or COPY_RESOURCE for controlled resources; COPY_NOTHING or COPY_REFERENCE for referenced resources")
  private CloningInstructionsEnum cloning;

  public CloningInstructionsEnum getCloning() {
    return cloning;
  }

  public boolean isDefined() {
    return cloning != null;
  }
}
