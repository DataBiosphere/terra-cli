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
    header = "Run applications in the workspace.",
    description =
        "The Terra CLI allows running supported third-party applications within the context of a workspace. "
            + "The `app-launch` config property controls how tools are run: either in a Docker container, or as a child process. \n\n"
            + "Nextflow and the `gcloud` SDK are the first examples of supported third-party applications.",
    subcommands = {Execute.class, List.class})
public class App {}
