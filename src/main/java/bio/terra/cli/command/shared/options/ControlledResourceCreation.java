package bio.terra.cli.command.shared.options;

import bio.terra.cli.serialization.userfacing.input.CreateResourceParams;
import bio.terra.workspace.model.AccessScope;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for create a new controlled Terra
 * resource: {@link ResourceCreation} and --access.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class ControlledResourceCreation {
  @CommandLine.Mixin ResourceCreation resourceCreationOption;

  @CommandLine.Option(
      names = "--access",
      description = "Access scope for the resource: ${COMPLETION-CANDIDATES}.")
  public AccessScope access = AccessScope.SHARED_ACCESS;

  /**
   * Helper method to return a {@link CreateResourceParams.Builder} with the controlled resource
   * metadata fields populated.
   */
  public CreateResourceParams.Builder populateMetadataFields() {
    return resourceCreationOption.populateMetadataFields().accessScope(access);
  }
}
