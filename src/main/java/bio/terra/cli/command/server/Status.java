package bio.terra.cli.command.server;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server status" command. */
@Command(name = "status", description = "Print status and details of the Terra server context.")
public class Status extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Update the Terra environment to which the CLI is pointing. */
  @Override
  protected void execute() {
    boolean serverIsOk = globalContext.server.ping();
    String serverIsOkMsg = serverIsOk ? "OKAY" : "ERROR CONNECTING";
    formatOption.printReturnValue(serverIsOkMsg, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(String returnValue) {
    OUT.println(
        "Current server: "
            + globalContext.server.name
            + " ("
            + globalContext.server.description
            + ")");
    OUT.println("Server status: " + returnValue);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
