package bio.terra.cli.command.datarefs;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs list" command. */
@CommandLine.Command(name = "list", description = "List all data references.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    java.util.List<CloudResource> dataReferences =
        new WorkspaceManager(globalContext, workspaceContext).listDataReferences();

    for (CloudResource dataReference : dataReferences) {
      System.out.println(
          dataReference.name + " (" + dataReference.type + "): " + dataReference.cloudId);
    }

    return 0;
  }
}
