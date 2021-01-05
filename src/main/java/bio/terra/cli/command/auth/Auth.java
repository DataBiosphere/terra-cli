package bio.terra.cli.command.auth;

import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra auth" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "auth",
    description = "Commands related to the retrieval and management of user credentials.",
    subcommands = {Login.class, Revoke.class, Status.class})
public class Auth {}
