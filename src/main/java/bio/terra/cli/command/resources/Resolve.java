package bio.terra.cli.command.resources;

import bio.terra.cli.command.resources.resolve.BqDataset;
import bio.terra.cli.command.resources.resolve.GcsBucket;
import picocli.CommandLine;

/**
 * This class corresponds to the third-level "terra resources resolve" command. This command is not
 * valid by itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "resolve",
    description = "Resolve a resource to its cloud id or path.",
    subcommands = {BqDataset.class, GcsBucket.class})
public class Resolve {}
