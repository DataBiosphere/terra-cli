package bio.terra.cli.command.app.passthrough;

import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra nextflow" command. */
@Command(
    name = "nextflow",
    description = "Use the nextflow tool in the Terra workspace.",
    hidden = true)
public class Nextflow implements Callable<Integer> {

  @CommandLine.Unmatched private List<String> cmdArgs;

  @Override
  public Integer call() throws Exception {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    new bio.terra.cli.apps.Nextflow(globalContext, workspaceContext).run(cmdArgs);

    return 0;
  }
}
