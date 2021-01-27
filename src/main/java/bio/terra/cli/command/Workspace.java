package bio.terra.cli.command;

import bio.terra.cli.command.workspace.AddUser;
import bio.terra.cli.command.workspace.Create;
import bio.terra.cli.command.workspace.Delete;
import bio.terra.cli.command.workspace.List;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra workspace" command. This command is not valid
 * by itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "workspace",
    description = "Commands related to the Terra workspace.",
    subcommands = {Create.class, List.class, Delete.class, AddUser.class})
public class Workspace {}
