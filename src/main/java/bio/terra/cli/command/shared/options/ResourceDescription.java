package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the --description option for `terra resources` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class ResourceDescription {
  @CommandLine.Option(names = "--description", description = "Description of the resource.")
  public String description;
}
