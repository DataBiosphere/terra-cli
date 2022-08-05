package bio.terra.cli.command;

import bio.terra.cli.command.workspace.AddUser;
import bio.terra.cli.command.workspace.BreakGlass;
import bio.terra.cli.command.workspace.Clone;
import bio.terra.cli.command.workspace.Create;
import bio.terra.cli.command.workspace.Delete;
import bio.terra.cli.command.workspace.DeleteProperty;
import bio.terra.cli.command.workspace.Describe;
import bio.terra.cli.command.workspace.List;
import bio.terra.cli.command.workspace.ListUsers;
import bio.terra.cli.command.workspace.RemoveUser;
import bio.terra.cli.command.workspace.Set;
import bio.terra.cli.command.workspace.SetProperty;
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
      AddUser.class,
      BreakGlass.class,
      Clone.class,
      Create.class,
      Delete.class,
      DeleteProperty.class,
      Describe.class,
      List.class,
      ListUsers.class,
      RemoveUser.class,
      Set.class,
      SetProperty.class,
      Update.class,
    })
public class Workspace {}
