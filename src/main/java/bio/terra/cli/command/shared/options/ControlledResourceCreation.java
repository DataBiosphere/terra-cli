package bio.terra.cli.command.shared.options;

import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.CloningInstructionsEnum;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for create a new controlled Terra
 * resource: {@link ResourceCreation} and --access.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class ControlledResourceCreation {
  @CommandLine.Option(
      names = "--access",
      description = "Access scope for the resource: ${COMPLETION-CANDIDATES}.")
  public AccessScope access = AccessScope.SHARED_ACCESS;
  // Cloning option must have a different default for referenced resources (REFERENCE) than
  // for controlled resources (RESOURCE).
  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}.")
  public CloningInstructionsEnum cloning = CloningInstructionsEnum.RESOURCE;
  @CommandLine.Mixin ResourceCreation resourceCreationOption;

  @CommandLine.Mixin ResourceCreation resourceCreationOption;

  /**
   * Helper method to return a {@link CreateResourceParams.Builder} with the controlled resource
   * metadata fields populated.
   */
  public CreateResourceParams.Builder populateMetadataFields() {
    return resourceCreationOption
        .populateMetadataFields()
        .accessScope(access)
        .cloningInstructions(cloning);
  }
}
