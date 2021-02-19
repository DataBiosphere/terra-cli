package bio.terra.cli.command.groups;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.utils.SamService;
import java.util.concurrent.Callable;
import org.broadinstitute.dsde.workbench.client.sam.model.ManagedGroupMembershipEntry;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra groups list" command. */
@Command(name = "list", description = "List the groups to which the current user belongs.")
public class List implements Callable<Integer> {
  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    java.util.List<ManagedGroupMembershipEntry> groups =
        new SamService(globalContext.server, globalContext.requireCurrentTerraUser()).listGroups();

    for (ManagedGroupMembershipEntry group : groups) {
      System.out.println(group.getGroupName());
    }

    return 0;
  }
}
