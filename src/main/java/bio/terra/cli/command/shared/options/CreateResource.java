package bio.terra.cli.command.shared.options;

import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateResource;
import bio.terra.workspace.model.CloningInstructionsEnum;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for create a new controlled or referenced
 * Terra resource: {@link ResourceName} and --description, --cloning.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class CreateResource extends ResourceName {
  @CommandLine.Option(names = "--description", description = "Description of the resource")
  public String description;

  @CommandLine.Option(
      names = "--cloning",
      description =
          "Instructions for handling when cloning the workspace: ${COMPLETION-CANDIDATES}")
  public CloningInstructionsEnum cloning = CloningInstructionsEnum.NOTHING;

  /**
   * Helper method to return a {@link CreateUpdateResource.Builder} with the resource metadata
   * fields populated.
   */
  public CreateUpdateResource.Builder populateMetadataFields() {
    return new CreateUpdateResource.Builder()
        .name(name)
        .description(description)
        .cloningInstructions(cloning);
  }
}
