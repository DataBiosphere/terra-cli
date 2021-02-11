package bio.terra.cli.command;

import bio.terra.cli.command.app.Execute;
import bio.terra.cli.command.app.GetImage;
import bio.terra.cli.command.app.List;
import bio.terra.cli.command.app.SetImage;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the second-level "terra app" command. This command is not valid by
 * itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "app",
    description = "Commands related to applications in the Terra workspace context.",
    subcommands = {List.class, GetImage.class, SetImage.class, Execute.class})
public class App {}
