package bio.terra.cli.command.resource;

import bio.terra.cli.command.resource.create.AwsS3StorageFolder;
import bio.terra.cli.command.resource.create.AwsSageMakerNotebook;
import bio.terra.cli.command.resource.create.BqDataset;
import bio.terra.cli.command.resource.create.GcpNotebook;
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
    subcommands = {
      GcsBucket.class,
      GcpNotebook.class,
      BqDataset.class,
      AwsS3StorageFolder.class,
      AwsSageMakerNotebook.class
    })
public class Create {}
