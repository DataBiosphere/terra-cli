package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.context.GlobalContext;
import java.util.concurrent.Callable;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get-value logging" command. */
@Command(name = "logging", description = "Get the logging level.")
public class Logging implements Callable<Integer> {
  private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Logging.class);

  @Override
  public Integer call() {
    GlobalContext globalContext = GlobalContext.readFromFile();

    System.out.println(
        "[console] logging level for printing directly to the terminal = "
            + globalContext.consoleLoggingLevel);
    System.out.println(
        "[file] logging level for writing to files in "
            + GlobalContext.getLogFile().getParent()
            + " = "
            + globalContext.fileLoggingLevel);

    return 0;
  }
}
