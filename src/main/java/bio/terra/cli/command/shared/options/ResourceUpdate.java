package bio.terra.cli.command.shared.options;

import bio.terra.cli.serialization.userfacing.input.UpdateResourceParams;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for updating a Terra resource: {@link
 * ResourceName}, --new-name, and {@link ResourceDescription}.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class ResourceUpdate {
  @CommandLine.Mixin public ResourceName resourceNameOption;

  @CommandLine.Option(
      names = "--new-name",
      description =
          "New name of the resource. Only alphanumeric and underscore characters are permitted.")
  public String newName;

  @CommandLine.Option(names = "--new-description", description = "New description of the resource.")
  public String newDescription;

  /**
   * Helper method to return a {@link UpdateResourceParams.Builder} with the resource metadata
   * fields populated.
   */
  public UpdateResourceParams.Builder populateMetadataFields() {
    return new UpdateResourceParams.Builder().name(newName).description(newDescription);
  }

  public boolean isDefined() {
    return newName != null || newDescription != null;
  }
}
