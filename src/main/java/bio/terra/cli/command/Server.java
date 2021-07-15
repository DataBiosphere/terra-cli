package bio.terra.cli.command;

import bio.terra.cli.command.server.List;
import bio.terra.cli.command.server.Set;
import bio.terra.cli.command.server.Status;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra server" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "server",
    description = "Connect to a Terra server.",
    subcommands = {List.class, Set.class, Status.class})
public class Server {}
