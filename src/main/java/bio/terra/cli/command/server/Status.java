package bio.terra.cli.command.server;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Server;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the third-level "terra server status" command. */
@Command(name = "status", description = "Print status and details of the Terra server context.")
public class Status extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(Status.class);

  @CommandLine.Mixin Format formatOption;

  /** Update the Terra environment to which the CLI is pointing. */
  @Override
  protected void execute() {
    logger.debug("terra server status");
    boolean serverIsOk = Context.getServer().ping();
    String serverIsOkMsg = serverIsOk ? "OKAY" : "ERROR CONNECTING";
    formatOption.printReturnValue(serverIsOkMsg, this::printText);
  }

  /** Print this command's output in text format. */
  private void printText(String returnValue) {
    Server server = Context.getServer();
    OUT.println("Current server: " + server.getName() + " (" + server.getDescription() + ")");
    OUT.println("Server status: " + returnValue);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
