package bio.terra.cli.command;

import bio.terra.cli.command.config.Get;
import bio.terra.cli.command.config.List;
import bio.terra.cli.command.config.Set;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra config" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "config",
    description = "Configure the CLI.",
    subcommands = {Get.class, List.class, Set.class})
public class Config {}
