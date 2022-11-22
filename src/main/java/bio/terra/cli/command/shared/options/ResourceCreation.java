package bio.terra.cli.command.shared.options;

import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for create a new controlled or referenced
 * Terra resource: {@link ResourceName} and {@link ResourceDescription}, --cloning.
 *
 * <p>This class is meant to be used as a {@link CommandLine.Mixin}.
 */
public class ResourceCreation {
  @CommandLine.Mixin ResourceName resourceNameOption;
  @CommandLine.Mixin ResourceDescription resourceDescriptionOption;

  /**
   * Helper method to return a {@link CreateResourceParams.Builder} with the resource metadata
   * fields populated.
   */
  public CreateResourceParams.Builder populateMetadataFields() {
    return new CreateResourceParams.Builder()
        .name(resourceNameOption.name)
        .description(resourceDescriptionOption.description);
  }
}
