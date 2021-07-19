package bio.terra.cli.command.resource;

import bio.terra.cli.command.resource.update.BqDataset;
import bio.terra.cli.command.resource.update.GcsBucket;
import picocli.CommandLine;

/**
 * This class corresponds to the third-level "terra resource update". This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "update",
    description = "Update the properties of a resource.",
    subcommands = {BqDataset.class, GcsBucket.class})
public class Update {}
