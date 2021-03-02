package bio.terra.cli.command.datarefs;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs delete" command. */
@CommandLine.Command(name = "delete", description = "Delete an existing data reference.")
public class Delete implements Callable<Integer> {

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
        new WorkspaceManager(globalContext, workspaceContext).deleteDataReference(name);

    System.out.println(
        dataReference.type + " reference successfully deleted: " + dataReference.cloudId);
    System.out.println("Workspace data reference successfully deleted: " + dataReference.name);

    return 0;
  }
}
