package bio.terra.cli.command;

import bio.terra.cli.command.resources.Create;
import bio.terra.cli.command.resources.Delete;
import bio.terra.cli.command.resources.Describe;
import bio.terra.cli.command.resources.List;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra resources" command. This command is not valid
 * by itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "resources",
    description = "Manage controlled resources in the workspace.",
    subcommands = {Create.class, Delete.class, Describe.class, List.class})
public class Resources {}
