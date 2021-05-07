package bio.terra.cli.command.config.set;

import bio.terra.cli.command.exception.UserActionableException;
import bio.terra.cli.command.helperclasses.BaseCommand;
import bio.terra.cli.context.GlobalContext;
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
        description = "maximum number to allow before throwing an error")
    private int max;

    @CommandLine.Option(
        names = "--default",
        description =
            "use the default number of resources: " + GlobalContext.DEFAULT_RESOURCES_CACHE_SIZE)
    private boolean useDefault;
  }

  /** Updates the resources cache size property of the global context. */
  @Override
  protected void execute() {
    int prevMaxResources = globalContext.resourcesCacheSize;
    int newMaxResources =
        argGroup.useDefault ? GlobalContext.DEFAULT_RESOURCES_CACHE_SIZE : argGroup.max;
    if (newMaxResources <= 0) {
      throw new UserActionableException(
          "Maximum number of resources allowed per workspace must be positive.");
    }
    globalContext.updateResourcesCacheSize(newMaxResources);

    if (globalContext.resourcesCacheSize == prevMaxResources) {
      OUT.println(
          "Max number of resources per workspace: "
              + globalContext.resourcesCacheSize
              + " (UNCHANGED)");
    } else {
      OUT.println(
          "Max number of resources per workspace: "
              + globalContext.resourcesCacheSize
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
