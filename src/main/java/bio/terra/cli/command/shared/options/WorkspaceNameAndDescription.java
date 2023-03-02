package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the workspace name and description options
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class WorkspaceNameAndDescription {
  @CommandLine.Option(
      names = "--name",
      required = false,
      description = "Workspace name (not unique).")
  public String name;

  @CommandLine.Option(
      names = "--description",
      required = false,
      description = "Workspace description.")
  public String description;
}
