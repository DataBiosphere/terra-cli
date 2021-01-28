package bio.terra.cli.command.app.supportedtools;

import bio.terra.cli.app.supportedtools.NextflowHelper;
import bio.terra.cli.model.GlobalContext;
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
    String cmdOutput = new NextflowHelper().run(globalContext, cmdArgs);
    System.out.println(cmdOutput);

    return 0;
  }
}
