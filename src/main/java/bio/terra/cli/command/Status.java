package bio.terra.cli.command;

import bio.terra.cli.Context;
import bio.terra.cli.Server;
import bio.terra.cli.Workspace;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.serialization.command.CommandServer;
import bio.terra.cli.serialization.command.CommandWorkspace;
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
        new StatusReturnValue(Context.getServer(), Context.getWorkspace().orElse(null));
    formatOption.printReturnValue(statusReturnValue, this::printText);
  }

  @SuppressFBWarnings(
      value = {"URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD"},
      justification = "This POJO class is used for easy serialization to JSON using Jackson.")
  /** POJO class for printing out this command's output. */
  @VisibleForTesting
  public static class StatusReturnValue {
    // global server context = service uris, environment name
    public final CommandServer server;

    // global workspace context
    public final CommandWorkspace workspace;

    public StatusReturnValue(Server server, Workspace workspace) {
      this.server = new CommandServer.Builder(server).build();
      this.workspace = workspace != null ? new CommandWorkspace.Builder(workspace).build() : null;
    }
  }

  /** Print this command's output in text format. */
  private void printText(StatusReturnValue returnValue) {
    // check if current workspace is defined
    if (returnValue.workspace == null) {
      OUT.println("There is no current Terra workspace defined.");
    } else {
      returnValue.workspace.print();
    }

    OUT.println();
    OUT.println("Terra server: " + returnValue.server.name);

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
