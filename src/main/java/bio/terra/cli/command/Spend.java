package bio.terra.cli.command;

import bio.terra.cli.command.spend.Disable;
import bio.terra.cli.command.spend.Enable;
import bio.terra.cli.command.spend.ListUsers;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra spend" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "spend",
    description = "Manage spend profiles.",
    subcommands = {Enable.class, Disable.class, ListUsers.class})
public class Spend {}
