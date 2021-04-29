package bio.terra.cli.command.helperclasses.options;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.workspace.model.AccessScope;
import bio.terra.workspace.model.ControlledResourceIamRole;
import java.util.List;
import picocli.CommandLine;

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
      description = "[PRIVATE ACCESS ONLY] IAM roles to grant user of private resource")
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
}
