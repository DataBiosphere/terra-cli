package bio.terra.cli.command.workspace;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.service.WorkspaceManager;
import bio.terra.workspace.model.RoleBinding;
import bio.terra.workspace.model.RoleBindingList;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra workspace list-users" command. */
@Command(name = "list-users", description = "List the users of the workspace.")
public class ListUsers extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** List all users of the workspace. */
  @Override
  protected void execute() {
    RoleBindingList roleBindings =
        new WorkspaceManager(globalContext, workspaceContext).listUsersOfWorkspace();
    formatOption.printReturnValue(roleBindings, ListUsers::printText);
  }

  /** Print this command's output in text format. */
  private static void printText(RoleBindingList returnValue) {
    for (RoleBinding roleBinding : returnValue) {
      OUT.println(roleBinding.getRole());
      for (String member : roleBinding.getMembers()) {
        OUT.println("  " + member);
      }
    }
  }
}
