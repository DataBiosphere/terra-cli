package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get-value browser" command. */
@Command(
    name = "browser",
    description = "Check whether a browser is launched automatically during the login process.")
public class Browser extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Return the browser launch option property of the global context. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(globalContext.getBrowserLaunchOption());
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
