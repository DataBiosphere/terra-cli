package bio.terra.cli.command.helperclasses.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the --name option for `terra resources` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class ResourceName {
  @CommandLine.Option(
      names = "--name",
      required = true,
      description =
          "Name of the resource, scoped to the workspace. Only alphanumeric and underscore characters are permitted.")
  public String name;
}
