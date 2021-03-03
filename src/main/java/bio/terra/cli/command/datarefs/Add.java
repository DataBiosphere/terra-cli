package bio.terra.cli.command.datarefs;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.CloudResource;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import bio.terra.cli.service.WorkspaceManager;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra data-refs add" command. */
@CommandLine.Command(name = "add", description = "Add a new data reference.")
public class Add implements Callable<Integer> {

  @CommandLine.Option(
      names = "--type",
      required = true,
      description = "The type of data reference to create: ${COMPLETION-CANDIDATES}")
  private CloudResource.Type type;

  @CommandLine.Option(
      names = "--name",
      required = true,
      description =
          "The name of the data reference, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
  private String name;

  @CommandLine.Option(
      names = "--uri",
      required = true,
      description = "The bucket path (e.g. gs://my-bucket)")
  private String uri;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    CloudResource dataReference =
        new WorkspaceManager(globalContext, workspaceContext).addDataReference(type, name, uri);

    System.out.println(
        "Workspace data reference successfully added: "
            + dataReference.name
            + " ("
            + dataReference.cloudId
            + ")");

    return 0;
  }
}
