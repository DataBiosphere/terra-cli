package bio.terra.cli.command;

import bio.terra.cli.command.datarefs.Add;
import bio.terra.cli.command.datarefs.CheckAccess;
import bio.terra.cli.command.datarefs.Delete;
import bio.terra.cli.command.datarefs.List;
import bio.terra.cli.command.datarefs.Resolve;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra data-refs" command. This command is not valid
 * by itself; it is just a grouping keyword for it sub-commands.
 */
@CommandLine.Command(
    name = "data-refs",
    description = "Reference data in the workspace context.",
    subcommands = {List.class, Add.class, Delete.class, CheckAccess.class, Resolve.class})
public class DataRefs {}
