package bio.terra.cli.command;

import bio.terra.cli.command.notebook.Create;
import bio.terra.cli.command.notebook.Delete;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra notebook" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "notebook",
    description = "Commands related to AI Notebooks in the Terra workspace context.",
    subcommands = {Create.class, Delete.class})
public class Notebook {}
