package bio.terra.cli.command.shared.options;

import java.util.Map;
import picocli.CommandLine;

/** @CommandLine.Mixin class for workspace name, description and Property options */
public class WorkspaceNameDescriptionAndProperty {
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

  @CommandLine.Option(
      names = "--properties",
      required = false,
      split = ",",
      description =
          "Workspace properties. Example: --properties=key=value. For multiple properties, use \",\": --properties=key1=value1,key2=value2")
  public Map<String, String> properties;
}
