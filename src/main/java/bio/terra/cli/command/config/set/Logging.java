package bio.terra.cli.command.config.set;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.context.utils.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set logging" command. */
@Command(name = "logging", description = "Set the logging level.")
public class Logging extends BaseCommand {
  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  Logging.LogLevelArgGroup argGroup;

  static class LogLevelArgGroup {
    @CommandLine.Option(names = "--console", description = "console logging level")
    private boolean console;

    @CommandLine.Option(names = "--file", description = "file logging level")
    private boolean file;
  }

  @CommandLine.Option(
      names = "--level",
      required = true,
      description = "logging level: ${COMPLETION-CANDIDATES}")
  private Logger.LogLevel level;

  /** Updates the logging level properties of the global context. */
  @Override
  protected void execute() {
    // note that this new log level will take effect on the NEXT command.
    // for the log level to take effect for the rest of THIS command, we'd need to re-initialize the
    // logger(s) (e.g. by calling Logger.setupLogging or some subset of that method). there's
    // currently no reason to update the log level in the same command because this command exits
    // immediately after updating the global context, so keeping it simple for now.
    if (argGroup.console) {
      globalContext.updateConsoleLoggingLevel(level);
      OUT.println("CONSOLE logging level set to: " + level);
    } else {
      globalContext.updateFileLoggingLevel(level);
      OUT.println("FILE logging level set to: " + level);
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
