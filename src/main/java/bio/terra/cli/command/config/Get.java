package bio.terra.cli.command.config;

import bio.terra.cli.command.config.get.AppLaunch;
import bio.terra.cli.command.config.get.Browser;
import bio.terra.cli.command.config.get.Image;
import bio.terra.cli.command.config.get.Logging;
import bio.terra.cli.command.config.get.ResourceLimit;
import bio.terra.cli.command.config.get.Server;
import bio.terra.cli.command.config.get.Workspace;
import picocli.CommandLine.Command;

/**
 * This class corresponds to the third-level "terra config get" command. This command is not valid
 * by itself; it is just a grouping keyword for it sub-commands.
 */
@Command(
    name = "get",
    description = "Get a configuration property value.",
    subcommands = {
      AppLaunch.class,
      Browser.class,
      Image.class,
      Logging.class,
      ResourceLimit.class,
      Server.class,
      Workspace.class
    })
public class Get {}
