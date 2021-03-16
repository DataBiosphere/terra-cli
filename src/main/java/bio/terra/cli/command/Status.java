package bio.terra.cli.command;

import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra status" command. */
@Command(name = "status", description = "Print details about the current workspace.")
public class Status implements Callable<Integer> {
  private static final Logger logger = LoggerFactory.getLogger(Status.class);

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    System.out.println("Terra server: " + globalContext.server.name);

    // check if current workspace is defined
    if (workspaceContext.isEmpty()) {
      System.out.println("There is no current Terra workspace defined.");
    } else {
      System.out.println("Terra workspace: " + workspaceContext.getWorkspaceId());
      System.out.println("Google project: " + workspaceContext.getGoogleProject());
    }

    return 0;
  }
}
