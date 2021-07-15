package bio.terra.cli.command;

import bio.terra.cli.command.app.Execute;
import bio.terra.cli.command.app.List;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra app" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "app",
    description = "Run applications in the workspace.",
    subcommands = {Execute.class, List.class})
public class App {}
