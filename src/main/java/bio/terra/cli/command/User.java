package bio.terra.cli.command;

import bio.terra.cli.command.user.Invite;
import bio.terra.cli.command.user.Status;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra user" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "user",
    description = "Manage users.",
    subcommands = {Invite.class, SshKey.class, Status.class})
public class User {}
