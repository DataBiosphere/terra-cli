package bio.terra.cli.command;

import bio.terra.cli.command.auth.Login;
import bio.terra.cli.command.auth.Revoke;
import bio.terra.cli.command.auth.Status;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra auth" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "auth",
    header = "Commands to manage user credentials.",
    description =
        "Only one user can be logged in at a time. Call terra auth login to login as a different user."
            + "asdfasdfasdf",
    subcommands = {Login.class, Revoke.class, Status.class})
public class Auth {}
