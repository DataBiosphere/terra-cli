package bio.terra.cli.command.resources;

import bio.terra.cli.command.resources.create.AiNotebook;
import bio.terra.cli.command.resources.create.BqDataset;
import bio.terra.cli.command.resources.create.GcsBucket;
import picocli.CommandLine;

/**
 * This class corresponds to the third-level "terra resources create" or "terra resources
 * create-controlled" command. This command is not valid by itself; it is just a grouping keyword
 * for it sub-commands.
 */
@CommandLine.Command(
    name = "create",
    aliases = {"create-controlled"},
    description = "Add a new controlled resource.",
    subcommands = {AiNotebook.class, BqDataset.class, GcsBucket.class})
public class Create {}
