package bio.terra.cli.command.config;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra config list" command. */
@Command(name = "list", description = "List all configuration properties.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() {
    System.out.println("terra config list");
    return 0;
  }
}
