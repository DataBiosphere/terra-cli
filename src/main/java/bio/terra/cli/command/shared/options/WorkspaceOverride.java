package bio.terra.cli.command.shared.options;

import bio.terra.cli.businessobject.Context;
import picocli.CommandLine;

/**
 * Command helper class that defines the --workspace flag for overriding the current workspace just
 * for this command.
 *
 * <p>Commands that use this option should call {@link #overrideIfSpecified()} before all other
 * business logic.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class WorkspaceOverride {
  @CommandLine.Option(
      names = "--workspace",
      description = "Workspace id to use for this command only.")
  // Variable is `id` instead of `userFacingId` because user sees it when they run a command with
  // missing required arguments
  private String id;

  /** Helper method to override the current workspace if the `--workspace` flag specifies an id. */
  public void overrideIfSpecified() {
    if (id != null && !id.isEmpty()) {
      Context.useOverrideWorkspace(id);
    }
  }
}
