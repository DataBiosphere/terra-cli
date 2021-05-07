package bio.terra.cli.command;

import bio.terra.cli.command.resources.AddRef;
import bio.terra.cli.command.resources.CheckAccess;
import bio.terra.cli.command.resources.Create;
import bio.terra.cli.command.resources.Delete;
import bio.terra.cli.command.resources.Describe;
import bio.terra.cli.command.resources.List;
import bio.terra.cli.command.resources.Resolve;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra resources" command. This command is not valid
 * by itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "resources",
    description = "Manage resources in the workspace.",
    subcommands = {
      AddRef.class,
      CheckAccess.class,
      Create.class,
      Delete.class,
      Describe.class,
      List.class,
      Resolve.class
    })
public class Resources {}
