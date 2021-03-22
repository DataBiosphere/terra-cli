package bio.terra.cli.command;

import bio.terra.cli.command.baseclasses.CommandWithFormatOptions;
import bio.terra.cli.context.ServerSpecification;
import bio.terra.workspace.model.WorkspaceDescription;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra status" command. */
@Command(name = "status", description = "Print details about the current workspace.")
public class Status extends CommandWithFormatOptions<Status.StatusReturnValue> {

  @Override
  protected StatusReturnValue execute() {
    return new StatusReturnValue(globalContext.server, workspaceContext.terraWorkspaceModel);
  }

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

  @Override
  protected void printText(StatusReturnValue returnValue) {
    out.println("Terra server: " + globalContext.server.name);

    // check if current workspace is defined
    if (workspaceContext.isEmpty()) {
      out.println("There is no current Terra workspace defined.");
    } else {
      out.println("Terra workspace: " + workspaceContext.getWorkspaceId());
      out.println("Google project: " + workspaceContext.getGoogleProject());
    }
  }

  @Override
  protected boolean requiresLogin() {
    // command never requires login
    return false;
  }
}
