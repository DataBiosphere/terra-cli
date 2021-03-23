package bio.terra.cli.command.config;

import bio.terra.cli.command.config.set.Browser;
import bio.terra.cli.command.config.set.Image;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the third-level "terra config set" command. This command is not valid
 * by itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "set",
    description = "Set a configuration property value.",
    subcommands = {Browser.class, Image.class})
public class Set {}
