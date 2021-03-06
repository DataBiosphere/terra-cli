package bio.terra.cli.command.shared.options;

import bio.terra.cli.service.SamService;
import picocli.CommandLine;

/**
 * Command helper class that defines the options for `terra group` commands that manage group
 * members.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class GroupMember {
  @CommandLine.Mixin public GroupName groupNameOption;

  @CommandLine.Option(
      names = "--email",
      required = true,
      description = "User (or other group) email.")
  public String email;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "Group policy: ${COMPLETION-CANDIDATES}.")
  public SamService.GroupPolicy policy;
}
