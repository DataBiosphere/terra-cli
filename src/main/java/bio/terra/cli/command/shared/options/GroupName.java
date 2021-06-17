package bio.terra.cli.command.shared.options;

import picocli.CommandLine;

/**
 * Command helper class that defines the --name option for `terra groups` commands.
 *
 * <p>This class is meant to be used as a @CommandLine.Mixin.
 */
public class GroupName {
  @CommandLine.Option(names = "--name", required = true, description = "Group name.")
  public String name;
}
