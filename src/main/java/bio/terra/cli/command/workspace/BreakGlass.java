package bio.terra.cli.command.workspace;

import bio.terra.cli.businessobject.Context;
import bio.terra.cli.command.shared.BaseCommand;
import bio.terra.cli.command.shared.options.WorkspaceOverride;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the third-level "terra workspace break-glass" command. This command is
 * hidden in the usage help.
 */
@Command(
    name = "break-glass",
    description = "Grant break-glass access to a workspace user.",
    hidden = true)
public class BreakGlass extends BaseCommand {

  @CommandLine.Option(
      names = "--email",
      required = true,
      description = "Email of workspace user requesting break-glass access.")
  private String granteeEmail;

  @CommandLine.Option(
      names = "--sa-key-file",
      required = true,
      description =
          "Path to the key file for a SA that has admin permissions on user projects for the current server.")
  private String saKeyFile;

  @CommandLine.Mixin WorkspaceOverride workspaceOption;

  /** Grant break-glass access to the workspace. */
  @Override
  protected void execute() {
    workspaceOption.overrideIfSpecified();

    Context.requireWorkspace().grantBreakGlass(granteeEmail, saKeyFile);

    OUT.println("Break-glass access successfully granted to: " + granteeEmail);
  }
}
