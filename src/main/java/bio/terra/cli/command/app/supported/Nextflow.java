package bio.terra.cli.command.app.supported;

import bio.terra.cli.apps.NextflowRunner;
import bio.terra.cli.auth.AuthenticationManager;
import bio.terra.cli.context.GlobalContext;
import bio.terra.cli.context.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra nextflow" command. */
@Command(
    name = "nextflow",
    description = "Use the nextflow tool in the Terra workspace.",
    hidden = true)
public class Nextflow implements Callable<Integer> {

  @CommandLine.Unmatched private String[] cmdArgs;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    NextflowRunner nextflowRunner = new NextflowRunner();
    nextflowRunner.setContext(globalContext, workspaceContext);
    String cmdOutput = nextflowRunner.run(cmdArgs);
    System.out.println(cmdOutput);

    return 0;
  }
}
