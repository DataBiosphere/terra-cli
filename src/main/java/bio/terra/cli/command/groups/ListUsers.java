package bio.terra.cli.command.groups;

import bio.terra.cli.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.service.SamService;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups list-users" command. */
@Command(name = "list-users", description = "List the users in a group with a given policy.")
public class ListUsers extends BaseCommand {
  @CommandLine.Parameters(index = "0", description = "The name of the group")
  private String group;

  @CommandLine.Option(
      names = "--policy",
      required = true,
      description = "The name of the policy: ${COMPLETION-CANDIDATES}")
  private SamService.GroupPolicy policy;

  @CommandLine.Mixin Format formatOption;

  /** List the groups to which the current user belongs. */
  @Override
  protected void execute() {
    List<String> users =
        new SamService(Context.getServer(), Context.requireUser()).listUsersInGroup(group, policy);
    formatOption.printReturnValue(users, ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<String> returnValue) {
    for (String user : returnValue) {
      OUT.println(user);
    }
  }
}
