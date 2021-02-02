package bio.terra.cli.command.app.supported;

import bio.terra.cli.app.AuthenticationManager;
import bio.terra.cli.app.supported.NextflowHelper;
import bio.terra.cli.model.GlobalContext;
import bio.terra.cli.model.WorkspaceContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra nextflow" command. */
@Command(name = "nextflow", description = "Use the nextflow tool in the Terra workspace.")
public class Nextflow implements Callable<Integer> {

  @CommandLine.Unmatched private String[] cmdArgs;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    WorkspaceContext workspaceContext = WorkspaceContext.readFromFile();

    new AuthenticationManager(globalContext, workspaceContext).loginTerraUser();
    String cmdOutput =
        new NextflowHelper().setContext(globalContext, workspaceContext).run(cmdArgs);
    System.out.println(cmdOutput);

    return 0;
  }
}
