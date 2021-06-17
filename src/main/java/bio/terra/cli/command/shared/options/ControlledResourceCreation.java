package bio.terra.cli.command.shared.options;

import bio.terra.cli.exception.UserActionableException;
import bio.terra.cli.serialization.userfacing.inputs.CreateUpdateResource;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.ControlledResourceIamRole;
import java.util.List;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for create a new controlled Terra
 * resource: {@link ResourceCreation} and --access, --email, --iam-roles.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class ControlledResourceCreation {
  @CommandLine.Mixin ResourceCreation resourceCreationOption;

  @CommandLine.Option(
      names = "--access",
      description = "Access scope for the resource: ${COMPLETION-CANDIDATES}")
  public AccessScope access = AccessScope.SHARED_ACCESS;

  @CommandLine.Option(
      names = "--email",
      description = "[PRIVATE ACCESS ONLY] Email address for user of private resource")
  public String privateUserEmail;

  @CommandLine.Option(
      names = "--iam-roles",
      split = ",",
      description =
          "[PRIVATE ACCESS ONLY] IAM roles to grant user of private resource: ${COMPLETION-CANDIDATES}")
  public List<ControlledResourceIamRole> privateIamRoles;

  /** Helper method to validate conditional required options. */
  public void validateAccessOptions() {
    if (access.equals(AccessScope.PRIVATE_ACCESS)) {
      if (privateUserEmail == null || privateUserEmail.isEmpty()) {
        throw new UserActionableException(
            "An email address (--email) is required for private resources.");
      }
      if (privateIamRoles == null || privateIamRoles.isEmpty()) {
        throw new UserActionableException(
            "IAM roles (--iam-roles) are required for private resources.");
      }
    }
  }

  /**
   * Helper method to return a {@link CreateUpdateResource.Builder} with the controlled resource
   * metadata fields populated.
   */
  public CreateUpdateResource.Builder populateMetadataFields() {
    return resourceCreationOption
        .populateMetadataFields()
        .accessScope(access)
        .privateUserName(privateUserEmail)
        .privateUserRoles(privateIamRoles);
  }
}
