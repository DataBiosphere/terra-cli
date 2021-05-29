package bio.terra.cli.command.helperclasses.options;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.context.Resource;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.ControlledResourceIamRole;
import java.util.List;
import picocli.CommandLine;

/**
 * Command helper class that defines the relevant options for create a new controlled Terra
 * resource: {@link CreateResource} and --access, --email, --iam-roles.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class CreateControlledResource extends CreateResource {
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
   * Helper method to populate a {@link bio.terra.cli.context.Resource.ResourceBuilder} with the
   * controlled resource metadata fields.
   */
  public void populateMetadataFields(Resource.ResourceBuilder builder) {
    super.populateMetadataFields(builder);
    builder.accessScope(access).privateUserName(privateUserEmail).privateUserRoles(privateIamRoles);
  }
}
