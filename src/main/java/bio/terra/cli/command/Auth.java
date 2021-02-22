package bio.terra.cli.command;

import bio.terra.cli.command.auth.GetBrowser;
import bio.terra.cli.command.auth.Login;
import bio.terra.cli.command.auth.Revoke;
import bio.terra.cli.command.auth.SetBrowser;
import bio.terra.cli.command.auth.Status;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra auth" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "auth",
    description = "Retrieve and manage user credentials.",
    subcommands = {Status.class, Login.class, Revoke.class, GetBrowser.class, SetBrowser.class})
public class Auth {}
