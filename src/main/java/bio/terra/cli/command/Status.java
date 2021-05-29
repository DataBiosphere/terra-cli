package bio.terra.cli.command;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import bio.terra.cli.context.Server;
import bio.terra.cli.context.Workspace;
import com.google.common.annotations.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra status" command. */
@Command(name = "status", description = "Print details about the current workspace and server.")
public class Status extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Build the return value from the global and workspace context. */
  @Override
  protected void execute() {
    StatusReturnValue statusReturnValue =
        new StatusReturnValue(
            globalContext.server, globalContext.getCurrentWorkspace().orElse(null));
    formatOption.printReturnValue(statusReturnValue, this::printText);
  }

  @SuppressFBWarnings(
      value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
      justification = "This POJO class is used for easy serialization to JSON using Jackson.")
  /** POJO class for printing out this command's output. */
  @VisibleForTesting
  public static class StatusReturnValue {
    // global server context = service uris, environment name
    public final Server server;

    // global workspace context
    public final Workspace workspace;

    public StatusReturnValue(Server server, Workspace workspace) {
      this.server = server;
      this.workspace = workspace;
    }
  }

  /** Print this command's output in text format. */
  private void printText(StatusReturnValue returnValue) {
    // check if current workspace is defined
    if (returnValue.workspace == null) {
      OUT.println("There is no current Terra workspace defined.");
    } else {
      returnValue.workspace.printText();
    }

    OUT.println();
    OUT.println("Terra server: " + globalContext.server.name);

    if (returnValue.workspace != null
        && !returnValue.server.name.equals(returnValue.workspace.serverName)) {
      OUT.println(
          "WARNING: The current workspace exists on a different server ("
              + returnValue.workspace.serverName
              + ") than the current one ("
              + returnValue.server.name
              + ").");
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
