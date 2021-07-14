package bio.terra.cli.command.shared.options;

import bio.terra.cli.serialization.userfacing.inputs.CreateResourceParams;
import bio.terra.workspace.model.CloningInstructionsEnum;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for create a new controlled or referenced
 * Terra resource: {@link ResourceName} and {@link ResourceDescription}, --cloning.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class ResourceCreation {
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin ResourceDescription resourceDescriptionOption;

  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}.")
  public CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;

  /**
   * Helper method to return a {@link CreateResourceParams.Builder} with the resource metadata
   * fields populated.
   */
  public CreateResourceParams.Builder populateMetadataFields() {
    return new CreateResourceParams.Builder()
        .name(resourceNameOption.name)
        .description(resourceDescriptionOption.description)
        .cloningInstructions(cloning);
  }
}
