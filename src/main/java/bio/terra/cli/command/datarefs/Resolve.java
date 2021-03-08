package bio.terra.cli.command.datarefs;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs resolve" command. */
@CommandLine.Command(
    name = "resolve",
    description = "Resolve a data reference to its cloud id or path.")
public class Resolve implements Callable<Integer> {

  @CommandLine.Option(
      names = "--name",
      required = true,
      description = "The name of the data reference, scoped to the workspace.")
  private String name;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    CloudResource dataReference =
        new WorkspaceManager(globalContext, workspaceContext).getDataReference(name);

    System.out.println(dataReference.cloudId);

    return 0;
  }
}
