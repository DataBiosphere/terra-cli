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
        "The Terra CLI authenticates users with the Google OAuth 2.0 installed application https://developers.google.com/identity/protocols/oauth2/native-app[flow]. \n\n"
            + "Only one user can be logged in at a time. Call `terra auth login` to login as a different user. You don't need to login again after switching workspaces. \n\n"
            + "You will need to login again after switching servers, because different Terra deployments may have different OAuth flows. \n\n"
            + "By default, the CLI opens a browser window for the user to click through the OAuth flow. For some use cases (e.g. CloudShell, notebook VM), this is not practical "
            + "because there is no default (or any) browser on the machine. The CLI has a browser option that controls this behavior: `terra config set browser MANUAL` means "
            + "the user can copy the URL into a browser on a different machine (e.g. their laptop), complete the login prompt, and then copy/paste the response token back into the shell prompt. \n\n",
    subcommands = {Login.class, Revoke.class, Status.class})
public class Auth {}
