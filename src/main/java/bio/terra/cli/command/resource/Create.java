package bio.terra.cli.command.resource;

import bio.terra.cli.command.resource.create.AiNotebook;
import bio.terra.cli.command.resource.create.BqDataset;
import bio.terra.cli.command.resource.create.GcsBucket;
import picocli.CommandLine;

/**
 * This class corresponds to the third-level "terra resource create" or "terra resource
 * create-controlled" command. This command is not valid by itself; it is just a grouping keyword
 * for it sub-commands.
 */
@CommandLine.Command(
    name = "create",
    aliases = {"create-controlled"},
    description = "Add a new controlled resource.",
    subcommands = {AiNotebook.class, BqDataset.class, GcsBucket.class})
public class Create {}
