package bio.terra.cli.command.shared.options;

import bio.terra.workspace.model.CloningInstructionsEnum;
import picocli.CommandLine;

public class CloningInstructionsForUpdate {
  /**
   * Use this --new-cloning flag when updating controlled resources.
   *
   * <p>This class is meant to be used as a @CommandLine.Mixin.
   */

  // Can't have a default. This is used for update. Need `null` in case user does not want to
  // update cloning instructions.
  @CommandLine.Option(
      names = "--new-cloning",
      // Only COPY_NOTHING，COPY_DEFINITION and COPY_RESOURCE are allowed for updating resources
      // Only COPY_NOTHING and COPY_REFERENCE are allowed for referenced resources
      // two options or commands in the same class file with same name could not build
      // https://github.com/DataBiosphere/terra-workspace-manager/blob/main/service/src/main/java/bio/terra/workspace/service/resource/ResourceValidationUtils.java#L452
      description =
          "Instructions for handling when cloning the workspace: COPY_NOTHING, COPY_DEFINITION, COPY_RESOURCE for controlled resources; COPY_NOTHING and COPY_REFERENCE are allowed for referenced resources")
  private CloningInstructionsEnum cloning;

  public CloningInstructionsEnum getCloning() {
    return cloning;
  }

  public boolean isDefined() {
    return cloning != null;
  }
}
