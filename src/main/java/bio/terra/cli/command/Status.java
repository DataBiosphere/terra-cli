package bio.terra.cli.command;

import bio.terra.cli.command.helperclasses.CommandSetup;
import bio.terra.cli.command.helperclasses.FormatFlag;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.workspace.model.WorkspaceDescription;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra status" command. */
@Command(name = "status", description = "Print details about the current workspace.")
public class Status extends CommandSetup {

  @CommandLine.Mixin FormatFlag formatFlag;

  /** Build the return value from the global and workspace context. */
  @Override
  protected void execute() {
    StatusReturnValue statusReturnValue =
        new StatusReturnValue(globalContext.server, workspaceContext.terraWorkspaceModel);
    formatFlag.printReturnValue(statusReturnValue, returnValue -> this.printText(returnValue));
  }

  /** POJO class for printing out this command's output. */
  public static class StatusReturnValue {
    // global server context = service uris, environment name
    public final ServerSpecification server;

    // workspace description object returned by WSM
    public final WorkspaceDescription workspace;

    public StatusReturnValue(ServerSpecification server, WorkspaceDescription workspace) {
      this.server = server;
      this.workspace = workspace;
    }
  }

  /**
   * Print this command's output in text format.
   *
   * @param returnValue command return value object
   */
  private void printText(StatusReturnValue returnValue) {
    out.println("Terra server: " + globalContext.server.name);

    // check if current workspace is defined
    if (workspaceContext.isEmpty()) {
      out.println("There is no current Terra workspace defined.");
    } else {
      out.println("Terra workspace: " + workspaceContext.getWorkspaceId());
      out.println("Google project: " + workspaceContext.getGoogleProject());
    }
  }

  /**
   * This command never requires login.
   *
   * @return false, always
   */
  @Override
  protected boolean doLogin() {
    return false;
  }
}
