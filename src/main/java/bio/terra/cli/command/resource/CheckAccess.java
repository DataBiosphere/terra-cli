package bio.terra.cli.command.resource;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.businessobject.Resource;
import bio.terra.cli.businessobject.User;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.Format;
import bio.terra.cli.command.shared.options.ResourceName;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/** This class corresponds to the third-level "terra resource check-access" command. */
@CommandLine.Command(
    name = "check-access",
    description = "Check if you have access to a referenced resource.")
public class CheckAccess extends BaseCommand {
  private static final Logger logger = LoggerFactory.getLogger(CheckAccess.class);
  @CommandLine.Mixin ResourceName resourceNameOption;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;
  @CommandLine.Mixin Format formatOption;

  /**
   * Check if the user and their proxy group have access to a referenced resource in the workspace.
   */
  @Override
  protected void execute() {
    logger.debug("terra resource check-access --name=" + resourceNameOption.name);
    workspaceOption.overrideIfSpecified();
    Resource resource = Context.requireWorkspace().getResource(resourceNameOption.name);
    boolean proxyGroupHasAccess = resource.checkAccess();
    formatOption.printReturnValue(proxyGroupHasAccess, this::printText);
  }

  /** Print this command's output in text format. */
  public void printText(boolean returnValue) {
    User currentUser = Context.requireUser();
    OUT.println(
        "User's pet SA in their proxy group ("
            + currentUser.getProxyGroupEmail()
            + ") DOES "
            + (returnValue ? "" : "NOT ")
            + "have access to this resource.");
  }
}
