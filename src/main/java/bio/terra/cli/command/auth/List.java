package bio.terra.cli.command.auth;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth list" command. */
@Command(name = "list", description = "Show all credentialed accounts.")
public class List implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    System.out.println("terra auth list");
    return 0;
  }
}
