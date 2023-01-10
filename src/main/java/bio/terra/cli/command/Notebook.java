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
    header = "Use Notebooks in the workspace.",
    description =
        "Commands to create and manage GCP Notebook resources within the workspace. \n\n"
            + "You can create a https://cloud.google.com/vertex-ai/docs/workbench/notebook-solution[GCP Notebook] controlled resource "
            + "with `terra resource create gcp-notebook`. The `stop`, `start` commands are provided for convenience. \n\n"
            + "You can also stop and start the notebook using the `gcloud notebooks instances start/stop` commands.",
    subcommands = {Start.class, Stop.class})
public class Notebook {}
