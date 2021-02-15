package bio.terra.cli.command.resources;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra resources describe" command. */
@Command(name = "describe", description = "Describe an existing controlled resource.")
public class Describe implements Callable<Integer> {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the resource, scoped to the workspace.")
  private String name;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    CloudResource resource =
        new WorkspaceManager(globalContext, workspaceContext).getControlledResource(name);

    System.out.println("Name: " + resource.name);
    System.out.println("Type: " + resource.type);
    System.out.println("Cloud Id: " + resource.cloudId);

    return 0;
  }
}
