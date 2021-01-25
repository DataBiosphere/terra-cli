package bio.terra.cli.command.server;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server set" command. */
@Command(name = "set", description = "Set the Terra server to connect to.")
public class Set implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "server name")
  private String serverName;

  @Override
  public Integer call() {
    System.out.println("terra server set: " + serverName);

    return 0;
  }
}
