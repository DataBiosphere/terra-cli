package bio.terra.cli.command;

import bio.terra.cli.command.cormwell.GenerateConfig;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra app" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "cormwell",
    description = "Run applications in the workspace.",
    subcommands = {GenerateConfig.class})
public class Cormwell {}
