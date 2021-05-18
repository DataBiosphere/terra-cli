package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get-value app-launch" command. */
@Command(name = "app-launch", description = "Check the way apps are launched.")
public class AppLaunch extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Return the command runner option property of the global context. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(globalContext.commandRunnerOption);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
