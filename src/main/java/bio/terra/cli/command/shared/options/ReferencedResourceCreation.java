package bio.terra.cli.command.shared.options;

import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.workspace.model.CloningInstructionsEnum;
import bio.terra.workspace.model.StewardshipType;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for creating a new referenced Terra
 * resource.
 *
 * <p>This class is meant to be used as a {@link CommandLine.Mixin}
 */
public class ReferencedResourceCreation {
  // Cloning option must have a different default for referenced resources (REFERENCE) than
  // for controlled resources (RESOURCE).
  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}.")
  public CloningInstructionsEnum cloningInstructionsOption = CloningInstructionsEnum.REFERENCE;
  @CommandLine.Mixin ResourceCreation resourceCreationOptions;

  /**
   * Helper method to return a {@link CreateResourceParams.Builder} with the referenced resource
   * metadata fields populated.
   */
  public CreateResourceParams.Builder populateMetadataFields() {
    return new CreateResourceParams.Builder()
        .name(resourceCreationOptions.resourceNameOption.name)
        .description(resourceCreationOptions.resourceDescriptionOption.description)
        .cloningInstructions(cloningInstructionsOption)
        .stewardshipType(StewardshipType.REFERENCED);
  }
}
