package bio.terra.cli.command;

import bio.terra.cli.command.config.Set;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra config" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "config",
    description = "Commands related to the configuration of the Terra CLI and current context.",
    subcommands = {Set.class})
public class Config {}
