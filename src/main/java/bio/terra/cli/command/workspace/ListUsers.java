package bio.terra.cli.command.workspace;

import bio.terra.cli.WorkspaceUser;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandWorkspaceUser;
import java.util.stream.Collectors;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace list-users" command. */
@Command(name = "list-users", description = "List the users of the workspace.")
public class ListUsers extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** List all users of the workspace. */
  @Override
  protected void execute() {
    java.util.List<WorkspaceUser> workspaceUsers = WorkspaceUser.list();
    formatOption.printReturnValue(
        workspaceUsers.stream()
            .map(workspaceUser -> new CommandWorkspaceUser(workspaceUser))
            .collect(Collectors.toList()),
        ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(java.util.List<CommandWorkspaceUser> returnValue) {
    for (CommandWorkspaceUser workspaceUser : returnValue) {
      workspaceUser.print();
    }
  }
}
