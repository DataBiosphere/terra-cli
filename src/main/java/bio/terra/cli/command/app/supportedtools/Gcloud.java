package bio.terra.cli.command.app.supportedtools;

import bio.terra.cli.app.supportedtools.GcloudHelper;
import bio.terra.cli.model.GlobalContext;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra gcloud" command. */
@Command(name = "gcloud", description = "Use the gcloud tool in the Terra workspace.")
public class Gcloud implements Callable<Integer> {

  @CommandLine.Unmatched private String[] cmdArgs;

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();
    String cmdOutput = new GcloudHelper().run(globalContext, cmdArgs);
    System.out.println(cmdOutput);

    return 0;
  }
}
