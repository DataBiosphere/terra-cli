package bio.terra.cli.command;

import bio.terra.cli.command.notebook.Launch;
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
        "Commands to manage Notebook resources within the workspace. \n\n"
            + "You can create a GCP Notebook controlled resource [https://cloud.google.com/vertex-ai/docs/workbench/notebook-solution]"
            + "with `terra resource create gcp-notebook`, and a AWS Notebook controlled resource [https://aws.amazon.com/sagemaker] "
            + "with `terra resource create sagemaker-notebook`. The `stop`, `start` and `launch` commands are provided for convenience. \n\n"
            + "You can also stop and start the gcp-notebook using the `gcloud notebooks instances start/stop` commands.",
    subcommands = {Start.class, Stop.class, Launch.class})
public class Notebook {}
