package bio.terra.cli.command;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the second-level "terra version" command. */
@Command(name = "version", description = "Get the installed version.")
public class Version implements Callable<Integer> {

  @Override
  public Integer call() {
    System.out.println(bio.terra.cli.context.utils.Version.getVersion());

    return 0;
  }
}
