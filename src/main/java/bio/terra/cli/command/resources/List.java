package bio.terra.cli.command.resources;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resources list" command. */
@Command(name = "list", description = "List all controlled resources.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    java.util.List<CloudResource> resources =
        new WorkspaceManager(globalContext, workspaceContext).listResources();

    for (CloudResource resource : resources) {
      System.out.println(resource.name + " (" + resource.type + "): " + resource.cloudId);
    }

    return 0;
  }
}
