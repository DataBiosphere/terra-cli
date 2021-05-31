package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get-value server" command. */
@Command(name = "server", description = "Get the Terra server the CLI connects to.")
public class Server extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Return the server property of the global context. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(
        globalContext.getServer(), bio.terra.cli.command.config.getvalue.Server::printText);
  }

  /** Print this command's output in text format. */
  public static void printText(bio.terra.cli.context.Server returnValue) {
    OUT.println(returnValue.name);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
