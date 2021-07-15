package bio.terra.cli.command;

import bio.terra.cli.command.groups.AddUser;
import bio.terra.cli.command.groups.Create;
import bio.terra.cli.command.groups.Delete;
import bio.terra.cli.command.groups.Describe;
import bio.terra.cli.command.groups.List;
import bio.terra.cli.command.groups.ListUsers;
import bio.terra.cli.command.groups.RemoveUser;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra groups" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "groups",
    description = "Manage groups of users.",
    subcommands = {
      AddUser.class,
      Create.class,
      Delete.class,
      Describe.class,
      List.class,
      ListUsers.class,
      RemoveUser.class
    })
public class Groups {}
