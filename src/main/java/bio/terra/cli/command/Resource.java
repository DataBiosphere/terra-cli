package bio.terra.cli.command;

import bio.terra.cli.command.resource.AddRef;
import bio.terra.cli.command.resource.CheckAccess;
import bio.terra.cli.command.resource.Create;
import bio.terra.cli.command.resource.Delete;
import bio.terra.cli.command.resource.Describe;
import bio.terra.cli.command.resource.List;
import bio.terra.cli.command.resource.Resolve;
import bio.terra.cli.command.resource.Update;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra resource" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "resource",
    description = "Manage resources in the workspace.",
    subcommands = {
      AddRef.class,
      CheckAccess.class,
      Create.class,
      Delete.class,
      Describe.class,
      List.class,
      Resolve.class,
      Update.class
    })
public class Resource {}
