package bio.terra.cli.command.config.set;

import bio.terra.cli.businessobject.Config;
import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.exception.UserActionableException;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/** This class corresponds to the fourth-level "terra config set resource-limit" command. */
@Command(
    name = "resource-limit",
    description = "Set the maximum number of resources allowed per workspace.")
public class ResourceLimit extends BaseCommand {

  @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
  ResourceLimitArgGroup argGroup;

  static class ResourceLimitArgGroup {
    @CommandLine.Option(
        names = "--max",
        description = "Maximum number to allow before throwing an error.")
    private int max;

    @CommandLine.Option(
        names = "--default",
        description =
            "Use the default number of resources: " + Config.DEFAULT_RESOURCES_CACHE_SIZE + ".")
    private boolean useDefault;
  }

  /** Updates the resources cache size property of the global context. */
  @Override
  protected void execute() {
    Config config = Context.getConfig();
    int prevMaxResources = config.getResourcesCacheSize();
    int newMaxResources = argGroup.useDefault ? Config.DEFAULT_RESOURCES_CACHE_SIZE : argGroup.max;
    if (newMaxResources <= 0) {
      throw new UserActionableException(
          "Maximum number of resources allowed per workspace must be positive.");
    }
    config.setResourcesCacheSize(newMaxResources);

    if (config.getResourcesCacheSize() == prevMaxResources) {
      OUT.println(
          "Max number of resources per workspace: "
              + config.getResourcesCacheSize()
              + " (UNCHANGED)");
    } else {
      OUT.println(
          "Max number of resources per workspace: "
              + config.getResourcesCacheSize()
              + " (CHANGED FROM "
              + prevMaxResources
              + ")");
    }
  }

  /** This command never requires login. */
  @Override
  protected boolean requiresLogin() {
    return false;
  }
}
