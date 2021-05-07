package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.command.helperclasses.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get-value resources" command. */
@Command(
    name = "resources",
    description = "Get the maximum number of resources allowed per workspace.")
public class Resources extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Return the resources cache size property of the global context. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(globalContext.resourcesCacheSize);
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
