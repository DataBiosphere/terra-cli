package bio.terra.cli.command;

import bio.terra.cli.command.notebooks.Get;
import bio.terra.cli.command.notebooks.Start;
import bio.terra.cli.command.notebooks.Stop;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra notebooks" command. This command is not valid
 * by itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "notebooks",
    description = "Use AI Notebooks in the workspace.",
    subcommands = {Get.class, Start.class, Stop.class})
public class Notebooks {}
