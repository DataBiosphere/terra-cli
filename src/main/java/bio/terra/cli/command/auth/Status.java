package bio.terra.cli.command.auth;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra auth status" command. */
@Command(name = "status", description = "Print details about the currently authorized account.")
public class Status implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    System.out.println("terra auth status");
    return 0;
  }
}
