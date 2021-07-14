package bio.terra.cli.command.shared.options;

import bio.terra.cli.businessobject.Context;
import java.util.UUID;
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
  private UUID id;

  /** Helper method to override the current workspace if the `--workspace` flag specifies an id. */
  public void overrideIfSpecified() {
    if (id != null) {
      Context.useOverrideWorkspace(id);
    }
  }
}
