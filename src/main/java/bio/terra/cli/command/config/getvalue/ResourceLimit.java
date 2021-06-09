package bio.terra.cli.command.config.getvalue;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get-value resource-limit" command. */
@Command(
    name = "resource-limit",
    description = "Get the maximum number of resources allowed per workspace.")
public class ResourceLimit extends BaseCommand {

  @CommandLine.Mixin Format formatOption;

  /** Return the resources cache size property of the global context. */
  @Override
  protected void execute() {
    formatOption.printReturnValue(Context.getConfig().getResourcesCacheSize());
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
