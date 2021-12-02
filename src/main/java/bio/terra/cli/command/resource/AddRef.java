package bio.terra.cli.command.resource;

import bio.terra.cli.command.resource.addref.BqDataTable;
import bio.terra.cli.command.resource.addref.BqDataset;
import bio.terra.cli.command.resource.addref.GcsBucket;
import bio.terra.cli.command.resource.addref.GcsFile;
import picocli.CommandLine;

/**
 * This class corresponds to the third-level "terra resource add-ref" or "terra resource
 * add-referenced" command. This command is not valid by itself; it is just a grouping keyword for
 * it sub-commands.
 */
@CommandLine.Command(
    name = "add-ref",
    aliases = {"add-referenced"},
    description = "Add a new referenced resource.",
    subcommands = {BqDataTable.class, BqDataset.class, GcsBucket.class, GcsFile.class})
public class AddRef {}
