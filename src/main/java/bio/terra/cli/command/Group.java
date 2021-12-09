package bio.terra.cli.command;

import bio.terra.cli.command.group.AddUser;
import bio.terra.cli.command.group.Create;
import bio.terra.cli.command.group.Delete;
import bio.terra.cli.command.group.Describe;
import bio.terra.cli.command.group.List;
import bio.terra.cli.command.group.ListUsers;
import bio.terra.cli.command.group.RemoveUser;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra group" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "group",
    header = "Manage groups of Terra users.",
    description =
        "Terra contains a directory-like system for managing groups of users. These commands are utility wrappers around the group management API endpoints. \n\n",
    subcommands = {
      AddUser.class,
      Create.class,
      Delete.class,
      Describe.class,
      List.class,
      ListUsers.class,
      RemoveUser.class
    })
public class Group {}
