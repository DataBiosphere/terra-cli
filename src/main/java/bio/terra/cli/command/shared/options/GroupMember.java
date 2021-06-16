package bio.terra.cli.command.shared.options;

import bio.terra.cli.service.SamService;
import picocli.CommandLine;

/**
 * Command helper class that defines the options for `terra groups` commands that manage group
 * members.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class GroupMember extends GroupName {
  @CommandLine.Option(
      names = "--email",
      required = true,
      description = "User (or other group) email.")
  public String email;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "Policy: ${COMPLETION-CANDIDATES}")
  public SamService.GroupPolicy policy;
}
