package bio.terra.cli.command.config;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra config set" command. */
@Command(name = "set", description = "Set a configuration property.")
public class Set implements Callable<Integer> {

  @CommandLine.Parameters(index = "0", description = "config property")
  private String configProperty;

  @CommandLine.Parameters(index = "1", description = "config value")
  private String configValue;

  @Override
  public Integer call() {
    System.out.println("terra config set: " + configProperty + " = " + configValue);

    return 0;
  }
}
