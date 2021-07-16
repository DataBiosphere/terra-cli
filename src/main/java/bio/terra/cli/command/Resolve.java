package bio.terra.cli.command;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import picocli.CommandLine;

/**
 * This class corresponds to the second-level "terra resolve" command. It is exactly the same
 * command as "terra resource resolve".
 */
@CommandLine.Command(name = "resolve", description = "Resolve a resource to its cloud id or path.")
@SuppressFBWarnings(
    value = "NM_SAME_SIMPLE_NAME_AS_SUPERCLASS",
    justification =
        "Command class names match the command name (e.g. Resolve -> terra resolve). In this case, we have a "
            + "command with a shortcut/alias that has the same name, but is up one level in the command hierarchy "
            + "(terra resolve = terra resource resolve).")
public class Resolve extends bio.terra.cli.command.resource.Resolve {}
