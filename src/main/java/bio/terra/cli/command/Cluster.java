package bio.terra.cli.command;

import bio.terra.cli.command.cluster.Launch;
import bio.terra.cli.command.cluster.Start;
import bio.terra.cli.command.cluster.Stop;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra cluster" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "cluster",
    header = "Use spark clusters in the workspace.",
    description =
        "Commands to start, stop, and launch cluster resources within the workspace. \n\n"
            + "You can create a GCP Dataproc cluster controlled resource [https://cloud.google.com/dataproc/docs/concepts/overview]",
    subcommands = {Start.class, Stop.class, Launch.class})
public class Cluster {}
