package bio.terra.cli.command.workspace;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.WorkspaceUser;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace list-users" command. */
@Command(name = "list-users", description = "List the users of the workspace.")
public class ListUsers extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** List all users of the workspace. */
  @Override
  protected void execute() {
    Map<String, WorkspaceUser> workspaceUsers = WorkspaceUser.list();
    formatOption.printReturnValue(workspaceUsers, ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(Map<String, WorkspaceUser> returnValue) {
    for (WorkspaceUser workspaceUser : returnValue.values()) {
      workspaceUser.printText();
    }
  }
}
