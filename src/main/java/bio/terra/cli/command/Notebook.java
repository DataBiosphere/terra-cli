package bio.terra.cli.command;

import bio.terra.cli.command.notebook.Start;
import bio.terra.cli.command.notebook.Stop;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra notebook" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "notebook",
    description = "Use Notebooks in the workspace.",
    subcommands = {Start.class, Stop.class})
public class Notebook {}
