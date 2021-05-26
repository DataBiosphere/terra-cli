package bio.terra.cli.command;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.PrintingUtils;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.workspace.model.WorkspaceDescription;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra status" command. */
@Command(name = "status", description = "Print details about the current workspace.")
public class Status extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Build the return value from the global and workspace context. */
  @Override
  protected void execute() {
    StatusReturnValue statusReturnValue =
        new StatusReturnValue(globalContext.server, workspaceContext.terraWorkspaceModel);
    formatOption.printReturnValue(statusReturnValue, this::printText);
  }

  @SuppressFBWarnings(
      value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
      justification = "This POJO class is used for easy serialization to JSON using Jackson.")
  /** POJO class for printing out this command's output. */
  @VisibleForTesting
  public static class StatusReturnValue {
    // global server context = service uris, environment name
    public ServerSpecification server;

    // workspace description object returned by WSM
    public WorkspaceDescription workspace;

    public StatusReturnValue() {}

    public StatusReturnValue(ServerSpecification server, WorkspaceDescription workspace) {
      this.server = server;
      this.workspace = workspace;
    }
  }

  /** Print this command's output in text format. */
  private void printText(StatusReturnValue returnValue) {
    OUT.println("Terra server: " + globalContext.server.name);

    // check if current workspace is defined
    if (workspaceContext.isEmpty()) {
      OUT.println("There is no current Terra workspace defined.");
    } else {
      PrintingUtils.printText(workspaceContext);
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
