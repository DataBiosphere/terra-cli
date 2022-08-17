package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * @CommandLine.Mixin class for workspace name and description options
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
