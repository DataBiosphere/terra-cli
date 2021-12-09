package bio.terra.cli.command.config.get;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config get resource-limit" command. */
@Command(
    name = "resource-limit",
    description = "Get the maximum number of resources allowed per workspace.")
public class ResourceLimit extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(ResourceLimit.class);

  @CommandLine.Mixin Format formatOption;

  /** Return the resources cache size property of the global context. */
  @Override
  protected void execute() {
    logger.debug("terra config get resource-limit");
    formatOption.printReturnValue(Context.getConfig().getResourcesCacheSize());
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
