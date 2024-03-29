package bio.terra.cli.command;

import bio.terra.cli.command.cromwell.GenerateConfig;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra cromwell" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "cromwell",
    description = "Commands related to Cromwell workflows.",
    subcommands = {GenerateConfig.class})
public class Cromwell {}
