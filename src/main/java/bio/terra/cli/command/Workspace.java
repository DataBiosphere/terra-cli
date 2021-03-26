package bio.terra.cli.command;

import bio.terra.cli.command.workspace.AddUser;
import bio.terra.cli.command.workspace.Create;
import bio.terra.cli.command.workspace.Delete;
import bio.terra.cli.command.workspace.List;
import bio.terra.cli.command.workspace.ListUsers;
import bio.terra.cli.command.workspace.Mount;
import bio.terra.cli.command.workspace.RemoveUser;
import bio.terra.cli.command.workspace.Update;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra workspace" command. This command is not valid
 * by itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "workspace",
    description = "Setup a Terra workspace.",
    subcommands = {
      List.class,
      Create.class,
      Mount.class,
      Delete.class,
      Update.class,
      ListUsers.class,
      AddUser.class,
      RemoveUser.class
    })
public class Workspace {}
