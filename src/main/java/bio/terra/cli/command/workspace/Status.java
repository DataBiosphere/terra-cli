package bio.terra.cli.command.workspace;

import bio.terra.cli.model.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth status" command. */
@Command(name = "status", description = "Print details about the current workspace context.")
public class Status implements Callable<Integer> {

  @Override
  public Integer call() {
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    // check if current workspace is defined
    if (workspaceContext.isEmpty()) {
      System.out.println("There is no current Terra workspace defined.");
    } else {
      System.out.println(
          "The current Terra workspace is "
              + workspaceContext.getWorkspaceId()
              + ", with backing Google project "
              + workspaceContext.getGoogleProject()
              + ".");
    }

    return 0;
  }
}
