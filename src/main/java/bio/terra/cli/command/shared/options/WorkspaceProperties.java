package bio.terra.cli.command.shared.options;

import java.util.Map;
import picocli.CommandLine;

/**
 * @CommandLine.Mixin class for workspace properties options
 */
public class WorkspaceProperties {
  @CommandLine.Option(
      names = "--properties",
      required = false,
      split = ",",
      description =
          "Workspace properties. Example: --properties=key=value. For multiple properties, use \",\": --properties=key1=value1,key2=value2")
  public Map<String, String> properties;
}
